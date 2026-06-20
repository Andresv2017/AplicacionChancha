<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$data   = json_decode(file_get_contents('php://input'), true);
$correo = trim($data['correo']     ?? '');
$pass   = trim($data['contrasena'] ?? '');

if (!$correo || !$pass) {
    http_response_code(400);
    echo json_encode(['error' => 'Correo y contrasena son obligatorios']);
    exit();
}

$conn = getConnection();

$stmt = $conn->prepare("SELECT id, nombre, contrasena FROM usuarios WHERE correo = ?");
$stmt->bind_param("s", $correo);
$stmt->execute();
$user = $stmt->get_result()->fetch_assoc();

if (!$user || !password_verify($pass, $user['contrasena'])) {
    http_response_code(401);
    echo json_encode(['error' => 'Correo o contrasena incorrectos']);
    exit();
}

// Generar token de sesion
$token = bin2hex(random_bytes(32));
$stmt  = $conn->prepare("UPDATE usuarios SET token_sesion = ? WHERE id = ?");
$stmt->bind_param("si", $token, $user['id']);
$stmt->execute();

echo json_encode([
    'mensaje'  => 'Login exitoso',
    'token'    => $token,
    'usuario'  => [
        'id'     => $user['id'],
        'nombre' => $user['nombre'],
        'correo' => $correo
    ]
]);

$conn->close();
