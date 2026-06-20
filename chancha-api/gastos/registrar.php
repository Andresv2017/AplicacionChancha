<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data         = json_decode(file_get_contents('php://input'), true);
$grupoId      = (int)($data['grupo_id']      ?? 0);
$descripcion  = trim($data['descripcion']    ?? '');
$montoTotal   = (float)($data['monto_total'] ?? 0);
$pagadoPorId  = (int)($data['pagado_por_id'] ?? 0);
$tipoDivision = $data['tipo_division']       ?? 'equitativa';
$fecha        = $data['fecha']               ?? date('Y-m-d');
$partes       = $data['partes']              ?? []; // [{usuario_id, monto_asignado, porcentaje?}]

// Validaciones basicas
if (!$grupoId || !$descripcion || $montoTotal <= 0 || !$pagadoPorId || empty($partes)) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos']);
    exit();
}

// Verificar que el usuario pertenece al grupo
$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $grupoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

// Validar que la suma de partes coincide con el total
$sumaPartes = array_sum(array_column($partes, 'monto_asignado'));
if (abs($sumaPartes - $montoTotal) > 0.01) {
    http_response_code(400);
    echo json_encode(['error' => 'La suma de las partes no coincide con el monto total']);
    exit();
}

$conn->begin_transaction();
try {
    $stmt = $conn->prepare(
        "INSERT INTO gastos (grupo_id, descripcion, monto_total, pagado_por_id, tipo_division, fecha)
         VALUES (?, ?, ?, ?, ?, ?)"
    );
    $stmt->bind_param("isdsss", $grupoId, $descripcion, $montoTotal, $pagadoPorId, $tipoDivision, $fecha);
    $stmt->execute();
    $gastoId = $conn->insert_id;

    // Dos statements: uno con porcentaje (modo porcentaje) y uno sin (otros modos)
    $stmtConPct = $conn->prepare(
        "INSERT INTO gasto_partes (gasto_id, usuario_id, monto_asignado, porcentaje, estado_pago)
         VALUES (?, ?, ?, ?, ?)"
    );
    $stmtSinPct = $conn->prepare(
        "INSERT INTO gasto_partes (gasto_id, usuario_id, monto_asignado, estado_pago)
         VALUES (?, ?, ?, ?)"
    );

    foreach ($partes as $parte) {
        $pUsuario    = (int)$parte['usuario_id'];
        $pMonto      = (float)$parte['monto_asignado'];
        $pPorcentaje = isset($parte['porcentaje']) ? (float)$parte['porcentaje'] : null;
        // Si el que pagó es el mismo, se marca como confirmado automáticamente
        $pEstado     = ($pUsuario === $pagadoPorId) ? 'confirmado' : 'pendiente';

        if ($pPorcentaje !== null) {
            $stmtConPct->bind_param("iidds", $gastoId, $pUsuario, $pMonto, $pPorcentaje, $pEstado);
            $stmtConPct->execute();
        } else {
            $stmtSinPct->bind_param("iids", $gastoId, $pUsuario, $pMonto, $pEstado);
            $stmtSinPct->execute();
        }

        // Crear notificacion para cada deudor (excepto quien pago)
        if ($pUsuario !== $pagadoPorId) {
            $msg   = "Nuevo gasto: $descripcion - Debes S/. " . number_format($pMonto, 2);
            $tipo  = 'deuda_nueva';
            $notif = $conn->prepare(
                "INSERT INTO notificaciones (usuario_id, tipo, mensaje) VALUES (?, ?, ?)"
            );
            $notif->bind_param("iss", $pUsuario, $tipo, $msg);
            $notif->execute();
        }
    }

    $conn->commit();
    http_response_code(201);
    echo json_encode(['mensaje' => 'Gasto registrado', 'gasto_id' => $gastoId]);

} catch (Exception $e) {
    $conn->rollback();
    http_response_code(500);
    echo json_encode(['error' => 'Error al registrar gasto']);
}

$conn->close();
