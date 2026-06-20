<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$grupoId = (int)($_GET['grupo_id'] ?? 0);
if (!$grupoId) {
    http_response_code(400);
    echo json_encode(['error' => 'grupo_id requerido']);
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

$stmt = $conn->prepare("
    SELECT
        c.id,
        c.descripcion,
        c.monto_por_persona,
        c.codigo_qr,
        c.estado,
        c.fecha_limite,
        c.creada_en,
        c.creador_id,
        (SELECT COUNT(*) FROM colecta_partes WHERE colecta_id = c.id) AS total_miembros,
        (SELECT COUNT(*) FROM colecta_partes
         WHERE colecta_id = c.id AND estado_pago IN ('pagado','confirmado')) AS pagados,
        (SELECT estado_pago FROM colecta_partes
         WHERE colecta_id = c.id AND usuario_id = ?) AS mi_estado
    FROM colectas c
    WHERE c.grupo_id = ?
    ORDER BY c.creada_en DESC
");
$stmt->bind_param("ii", $userId, $grupoId);
$stmt->execute();
$result = $stmt->get_result();

$colectas = [];
while ($row = $result->fetch_assoc()) {
    $colectas[] = $row;
}

echo json_encode(['colectas' => $colectas]);
$conn->close();
