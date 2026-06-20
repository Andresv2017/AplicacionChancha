<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data    = json_decode(file_get_contents('php://input'), true);
$gastoId = (int)($data['gasto_id'] ?? 0);

if (!$gastoId) {
    http_response_code(400);
    echo json_encode(['error' => 'gasto_id requerido']);
    exit();
}

// Verificar que la parte le pertenece al usuario y está pendiente
$check = $conn->prepare("
    SELECT gp.id FROM gasto_partes gp
    JOIN gastos g ON g.id = gp.gasto_id
    WHERE gp.gasto_id    = ?
      AND gp.usuario_id  = ?
      AND gp.estado_pago = 'pendiente'
      AND gp.usuario_id != g.pagado_por_id
");
$check->bind_param("ii", $gastoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(404);
    echo json_encode(['error' => 'Deuda no encontrada o ya pagada']);
    exit();
}

$stmt = $conn->prepare("
    UPDATE gasto_partes SET estado_pago = 'confirmado'
    WHERE gasto_id = ? AND usuario_id = ?
");
$stmt->bind_param("ii", $gastoId, $userId);
$stmt->execute();

if ($stmt->affected_rows > 0) {
    echo json_encode(['mensaje' => 'Pago confirmado']);
} else {
    http_response_code(500);
    echo json_encode(['error' => 'No se pudo confirmar el pago']);
}

$conn->close();
