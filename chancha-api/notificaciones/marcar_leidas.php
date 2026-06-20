<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$stmt = $conn->prepare("UPDATE notificaciones SET leida = 1 WHERE usuario_id = ?");
$stmt->bind_param("i", $userId);
$stmt->execute();

echo json_encode(['mensaje' => 'Notificaciones marcadas como leidas']);
$conn->close();
