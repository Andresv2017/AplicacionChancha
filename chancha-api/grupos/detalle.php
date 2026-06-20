<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn    = getConnection();
$userId  = getUserIdFromToken($conn);
$grupoId = (int)($_GET['grupo_id'] ?? 0);

if (!$grupoId) {
    http_response_code(400);
    echo json_encode(['error' => 'grupo_id requerido']);
    exit();
}

$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $grupoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

$stmt = $conn->prepare("
    SELECT g.id, g.nombre, g.descripcion, g.codigo_invitacion, g.fecha_creacion,
           g.creado_por_id,
           u.nombre AS creador_nombre,
           (SELECT COUNT(*) FROM grupo_miembros gm WHERE gm.grupo_id = g.id) AS num_miembros
    FROM grupos g
    JOIN usuarios u ON u.id = g.creado_por_id
    WHERE g.id = ?
");
$stmt->bind_param("i", $grupoId);
$stmt->execute();
$grupo = $stmt->get_result()->fetch_assoc();

echo json_encode(['grupo' => $grupo]);
$conn->close();
