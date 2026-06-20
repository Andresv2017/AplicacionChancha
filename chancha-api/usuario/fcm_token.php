<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$userId   = getUserIdFromToken(getConnection());
$data     = json_decode(file_get_contents('php://input'), true);
$fcmToken = trim($data['fcm_token'] ?? '');

if (!$fcmToken) {
    http_response_code(400);
    echo json_encode(['error' => 'Token FCM requerido']);
    exit();
}

$conn = getConnection();
$stmt = $conn->prepare("UPDATE usuarios SET fcm_token = ? WHERE id = ?");
$stmt->bind_param("si", $fcmToken, $userId);
$stmt->execute();

echo json_encode(['mensaje' => 'Token FCM actualizado']);
$conn->close();
