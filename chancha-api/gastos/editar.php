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
$gastoId      = (int)($data['gasto_id']      ?? 0);
$descripcion  = trim($data['descripcion']    ?? '');
$montoTotal   = (float)($data['monto_total'] ?? 0);
$pagadoPorId  = (int)($data['pagado_por_id'] ?? 0);
$tipoDivision = $data['tipo_division']       ?? null;
$partes       = $data['partes']              ?? [];

if (!$gastoId || !$descripcion || $montoTotal <= 0 || !$pagadoPorId || empty($partes)) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos']);
    exit();
}

// Verificar que el gasto existe y obtener grupo_id
$stmtG = $conn->prepare("SELECT grupo_id FROM gastos WHERE id = ?");
$stmtG->bind_param("i", $gastoId);
$stmtG->execute();
$gastoRow = $stmtG->get_result()->fetch_assoc();
if (!$gastoRow) {
    http_response_code(404);
    echo json_encode(['error' => 'Gasto no encontrado']);
    exit();
}
$grupoId = $gastoRow['grupo_id'];

// Verificar que el usuario es admin del grupo
$stmtAdmin = $conn->prepare("SELECT creado_por_id FROM grupos WHERE id = ?");
$stmtAdmin->bind_param("i", $grupoId);
$stmtAdmin->execute();
$grupo = $stmtAdmin->get_result()->fetch_assoc();
if (!$grupo || $grupo['creado_por_id'] != $userId) {
    http_response_code(403);
    echo json_encode(['error' => 'Solo el admin puede editar gastos']);
    exit();
}

// Validar suma de partes
$sumaPartes = array_sum(array_column($partes, 'monto_asignado'));
if (abs($sumaPartes - $montoTotal) > 0.01) {
    http_response_code(400);
    echo json_encode(['error' => 'La suma de las partes no coincide con el monto total']);
    exit();
}

$conn->begin_transaction();
try {
    // Actualizar gasto (con tipo_division si fue enviado)
    if ($tipoDivision !== null) {
        $stmt = $conn->prepare(
            "UPDATE gastos SET descripcion = ?, monto_total = ?, pagado_por_id = ?, tipo_division = ? WHERE id = ?"
        );
        $stmt->bind_param("sdssi", $descripcion, $montoTotal, $pagadoPorId, $tipoDivision, $gastoId);
    } else {
        $stmt = $conn->prepare(
            "UPDATE gastos SET descripcion = ?, monto_total = ?, pagado_por_id = ? WHERE id = ?"
        );
        $stmt->bind_param("sdii", $descripcion, $montoTotal, $pagadoPorId, $gastoId);
    }
    $stmt->execute();

    // Eliminar partes anteriores (una sola vez, correctamente)
    $del = $conn->prepare("DELETE FROM gasto_partes WHERE gasto_id = ?");
    $del->bind_param("i", $gastoId);
    $del->execute();

    // Reinsertar partes (con porcentaje si fue enviado)
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
        $pEstado     = ($pUsuario === $pagadoPorId) ? 'confirmado' : 'pendiente';
        if ($pPorcentaje !== null) {
            $stmtConPct->bind_param("iidds", $gastoId, $pUsuario, $pMonto, $pPorcentaje, $pEstado);
            $stmtConPct->execute();
        } else {
            $stmtSinPct->bind_param("iids", $gastoId, $pUsuario, $pMonto, $pEstado);
            $stmtSinPct->execute();
        }
    }

    $conn->commit();
    echo json_encode(['mensaje' => 'Gasto actualizado']);
} catch (Exception $e) {
    $conn->rollback();
    http_response_code(500);
    echo json_encode(['error' => 'Error al actualizar gasto']);
}

$conn->close();
