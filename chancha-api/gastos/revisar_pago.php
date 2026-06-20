<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data     = json_decode(file_get_contents('php://input'), true);
$gastoId  = (int)($data['gasto_id']  ?? 0);
$deudorId = (int)($data['deudor_id'] ?? 0);
$accion   = $data['accion'] ?? '';

if (!$gastoId || !$deudorId || !in_array($accion, ['confirmar', 'rechazar'])) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos']);
    exit();
}

// Verificar que el usuario es el acreedor (quien pagó el gasto)
$check = $conn->prepare("SELECT id FROM gastos WHERE id = ? AND pagado_por_id = ?");
$check->bind_param("ii", $gastoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No eres el acreedor de este gasto']);
    exit();
}

// Verificar que el comprobante está en revisión
$check2 = $conn->prepare("
    SELECT id FROM gasto_partes
    WHERE gasto_id = ? AND usuario_id = ? AND estado_pago = 'en_revision'
");
$check2->bind_param("ii", $gastoId, $deudorId);
$check2->execute();
if ($check2->get_result()->num_rows === 0) {
    http_response_code(404);
    echo json_encode(['error' => 'Comprobante no encontrado o no está en revisión']);
    exit();
}

if ($accion === 'confirmar') {
    $stmt = $conn->prepare("
        UPDATE gasto_partes SET estado_pago = 'confirmado'
        WHERE gasto_id = ? AND usuario_id = ?
    ");
    $stmt->bind_param("ii", $gastoId, $deudorId);
    $stmt->execute();
    echo json_encode(['mensaje' => 'Pago confirmado']);
} else {
    // Rechazar: vuelve a pendiente y se limpia el comprobante para resubir
    $stmt = $conn->prepare("
        UPDATE gasto_partes SET estado_pago = 'pendiente', comprobante = NULL
        WHERE gasto_id = ? AND usuario_id = ?
    ");
    $stmt->bind_param("ii", $gastoId, $deudorId);
    $stmt->execute();
    echo json_encode(['mensaje' => 'Pago rechazado']);
}

$conn->close();
