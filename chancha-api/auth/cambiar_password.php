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
$passwordActual  = $data['password_actual']  ?? '';
$passwordNueva   = $data['password_nueva']   ?? '';

if (!$passwordActual || !$passwordNueva) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos']);
    exit();
}

if (strlen($passwordNueva) < 6) {
    http_response_code(400);
    echo json_encode(['error' => 'La nueva contraseña debe tener al menos 6 caracteres']);
    exit();
}

// Verificar contraseña actual
$stmt = $conn->prepare("SELECT contrasena FROM usuarios WHERE id = ?");
$stmt->bind_param("i", $userId);
$stmt->execute();
$user = $stmt->get_result()->fetch_assoc();

if (!$user || !password_verify($passwordActual, $user['contrasena'])) {
    http_response_code(401);
    echo json_encode(['error' => 'La contraseña actual es incorrecta']);
    exit();
}

// Actualizar con nuevo hash
$nuevoHash = password_hash($passwordNueva, PASSWORD_DEFAULT);
$upd = $conn->prepare("UPDATE usuarios SET contrasena = ? WHERE id = ?");
$upd->bind_param("si", $nuevoHash, $userId);
$upd->execute();

echo json_encode(['mensaje' => 'Contraseña actualizada correctamente']);
$conn->close();
