<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$stmt = $conn->prepare("UPDATE usuarios SET token_sesion = NULL WHERE id = ?");
$stmt->bind_param("i", $userId);
$stmt->execute();

echo json_encode(['mensaje' => 'Sesion cerrada correctamente']);
$conn->close();
