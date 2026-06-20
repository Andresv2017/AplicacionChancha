<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data            = json_decode(file_get_contents('php://input'), true);
$grupoId         = (int)($data['grupo_id']          ?? 0);
$descripcion     = trim($data['descripcion']         ?? '');
$montoPorPersona = (float)($data['monto_por_persona'] ?? 0);
$fechaLimite     = $data['fecha_limite']             ?? null;

if (!$grupoId || !$descripcion || $montoPorPersona <= 0) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos']);
    exit();
}

// Solo el admin del grupo puede crear colectas
$stmtAdmin = $conn->prepare("SELECT creado_por_id FROM grupos WHERE id = ?");
$stmtAdmin->bind_param("i", $grupoId);
$stmtAdmin->execute();
$grupo = $stmtAdmin->get_result()->fetch_assoc();
if (!$grupo || $grupo['creado_por_id'] != $userId) {
    http_response_code(403);
    echo json_encode(['error' => 'Solo el admin puede crear colectas']);
    exit();
}

// Obtener todos los miembros del grupo
$stmtMiembros = $conn->prepare("SELECT usuario_id FROM grupo_miembros WHERE grupo_id = ?");
$stmtMiembros->bind_param("i", $grupoId);
$stmtMiembros->execute();
$miembros = $stmtMiembros->get_result()->fetch_all(MYSQLI_ASSOC);

if (empty($miembros)) {
    http_response_code(400);
    echo json_encode(['error' => 'El grupo no tiene miembros']);
    exit();
}

// Generar código QR único (UUID v4)
$codigoQr = sprintf(
    '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
    mt_rand(0, 0xffff), mt_rand(0, 0xffff),
    mt_rand(0, 0xffff),
    mt_rand(0, 0x0fff) | 0x4000,
    mt_rand(0, 0x3fff) | 0x8000,
    mt_rand(0, 0xffff), mt_rand(0, 0xffff), mt_rand(0, 0xffff)
);

$conn->begin_transaction();
try {
    // Insertar colecta
    if ($fechaLimite) {
        $stmt = $conn->prepare(
            "INSERT INTO colectas (grupo_id, descripcion, monto_por_persona, codigo_qr, creador_id, fecha_limite)
             VALUES (?, ?, ?, ?, ?, ?)"
        );
        $stmt->bind_param("isdsis", $grupoId, $descripcion, $montoPorPersona, $codigoQr, $userId, $fechaLimite);
    } else {
        $stmt = $conn->prepare(
            "INSERT INTO colectas (grupo_id, descripcion, monto_por_persona, codigo_qr, creador_id)
             VALUES (?, ?, ?, ?, ?)"
        );
        $stmt->bind_param("isdsi", $grupoId, $descripcion, $montoPorPersona, $codigoQr, $userId);
    }
    $stmt->execute();
    $colectaId = $conn->insert_id;

    // Insertar parte por cada miembro
    $stmtParte = $conn->prepare(
        "INSERT INTO colecta_partes (colecta_id, usuario_id) VALUES (?, ?)"
    );
    foreach ($miembros as $miembro) {
        $uid = $miembro['usuario_id'];
        $stmtParte->bind_param("ii", $colectaId, $uid);
        $stmtParte->execute();
    }

    // Notificar a todos los miembros excepto el creador
    $msg   = "Nueva colecta: $descripcion - S/. " . number_format($montoPorPersona, 2) . " por persona";
    $tipo  = 'colecta_nueva';
    $notif = $conn->prepare("INSERT INTO notificaciones (usuario_id, tipo, mensaje) VALUES (?, ?, ?)");
    foreach ($miembros as $miembro) {
        $uid = $miembro['usuario_id'];
        if ($uid != $userId) {
            $notif->bind_param("iss", $uid, $tipo, $msg);
            $notif->execute();
        }
    }

    $conn->commit();
    http_response_code(201);
    echo json_encode([
        'mensaje'    => 'Colecta creada',
        'colecta_id' => $colectaId,
        'codigo_qr'  => $codigoQr
    ]);

} catch (Exception $e) {
    $conn->rollback();
    http_response_code(500);
    echo json_encode(['error' => 'Error al crear colecta']);
}

$conn->close();
