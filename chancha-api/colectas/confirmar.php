<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data      = json_decode(file_get_contents('php://input'), true);
$colectaId = (int)($data['colecta_id'] ?? 0);
$pagadorId = (int)($data['usuario_id'] ?? 0);
$accion    = $data['accion'] ?? 'confirmar'; // 'confirmar' o 'rechazar'

if (!$colectaId || !$pagadorId || !in_array($accion, ['confirmar', 'rechazar'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos']);
    exit();
}

// Verificar que el usuario es admin del grupo de la colecta
$stmtColecta = $conn->prepare("
    SELECT c.descripcion, g.creado_por_id
    FROM colectas c
    JOIN grupos g ON g.id = c.grupo_id
    WHERE c.id = ?
");
$stmtColecta->bind_param("i", $colectaId);
$stmtColecta->execute();
$colecta = $stmtColecta->get_result()->fetch_assoc();

if (!$colecta) {
    http_response_code(404);
    echo json_encode(['error' => 'Colecta no encontrada']);
    exit();
}

if ($colecta['creado_por_id'] != $userId) {
    http_response_code(403);
    echo json_encode(['error' => 'Solo el admin puede confirmar pagos']);
    exit();
}

// Verificar que la parte está en estado 'en_revision'
$stmtParte = $conn->prepare(
    "SELECT id FROM colecta_partes WHERE colecta_id = ? AND usuario_id = ? AND estado_pago = 'en_revision'"
);
$stmtParte->bind_param("ii", $colectaId, $pagadorId);
$stmtParte->execute();
$parte = $stmtParte->get_result()->fetch_assoc();

if (!$parte) {
    http_response_code(409);
    echo json_encode(['error' => 'No hay comprobante pendiente de revision']);
    exit();
}

if ($accion === 'confirmar') {
    $stmt = $conn->prepare(
        "UPDATE colecta_partes SET estado_pago = 'confirmado' WHERE id = ?"
    );
    $stmt->bind_param("i", $parte['id']);
    $stmt->execute();

    $msg  = "Tu pago en la colecta '" . $colecta['descripcion'] . "' fue confirmado ✓";
    $tipo = 'pago_confirmado';
    $notif = $conn->prepare("INSERT INTO notificaciones (usuario_id, tipo, mensaje) VALUES (?, ?, ?)");
    $notif->bind_param("iss", $pagadorId, $tipo, $msg);
    $notif->execute();
    echo json_encode(['mensaje' => 'Pago confirmado']);
} else {
    // Rechazar: vuelve a pendiente, limpia comprobante para que reenvíe
    $stmt = $conn->prepare(
        "UPDATE colecta_partes SET estado_pago = 'pendiente', comprobante = NULL WHERE id = ?"
    );
    $stmt->bind_param("i", $parte['id']);
    $stmt->execute();

    $msg  = "Tu comprobante en '" . $colecta['descripcion'] . "' fue rechazado. Por favor reenvía.";
    $tipo = 'pago_rechazado';
    $notif = $conn->prepare("INSERT INTO notificaciones (usuario_id, tipo, mensaje) VALUES (?, ?, ?)");
    $notif->bind_param("iss", $pagadorId, $tipo, $msg);
    $notif->execute();
    echo json_encode(['mensaje' => 'Comprobante rechazado']);
}
$conn->close();
