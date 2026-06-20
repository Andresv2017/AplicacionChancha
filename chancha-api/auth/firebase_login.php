<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$data          = json_decode(file_get_contents('php://input'), true);
$firebaseToken = trim($data['firebase_token'] ?? '');
$nombreExtra   = trim($data['nombre'] ?? '');

if (!$firebaseToken) {
    http_response_code(400);
    echo json_encode(['error' => 'Token de Firebase requerido']);
    exit();
}

// Decodificar JWT de Firebase (payload es base64url)
$partes = explode('.', $firebaseToken);
if (count($partes) !== 3) {
    http_response_code(401);
    echo json_encode(['error' => 'Token invalido']);
    exit();
}

$payload = $partes[1];
$payload = str_replace(['-', '_'], ['+', '/'], $payload);
$padding = strlen($payload) % 4;
if ($padding) $payload .= str_repeat('=', 4 - $padding);
$tokenData = json_decode(base64_decode($payload), true);

if (!isset($tokenData['email'])) {
    http_response_code(401);
    echo json_encode(['error' => 'Token invalido: falta email']);
    exit();
}

// Verificar que no haya expirado
if (isset($tokenData['exp']) && $tokenData['exp'] < time()) {
    http_response_code(401);
    echo json_encode(['error' => 'Token expirado']);
    exit();
}

$correo         = $tokenData['email'];
$nombreFirebase = $nombreExtra ?: ($tokenData['name'] ?? explode('@', $correo)[0]);

$conn = getConnection();

// Buscar usuario por correo
$stmt = $conn->prepare("SELECT id, nombre FROM usuarios WHERE correo = ?");
$stmt->bind_param("s", $correo);
$stmt->execute();
$user = $stmt->get_result()->fetch_assoc();

if (!$user) {
    // Crear usuario nuevo (primer login con Google)
    $stmt = $conn->prepare("INSERT INTO usuarios (nombre, correo, contrasena) VALUES (?, ?, '')");
    $stmt->bind_param("ss", $nombreFirebase, $correo);
    if (!$stmt->execute()) {
        http_response_code(500);
        echo json_encode(['error' => 'Error al crear usuario']);
        exit();
    }
    $userId      = $conn->insert_id;
    $nombreFinal = $nombreFirebase;
} else {
    $userId      = $user['id'];
    $nombreFinal = $user['nombre'];
}

// Generar token de sesion para el backend
$token = bin2hex(random_bytes(32));
$stmt  = $conn->prepare("UPDATE usuarios SET token_sesion = ? WHERE id = ?");
$stmt->bind_param("si", $token, $userId);
$stmt->execute();

echo json_encode([
    'mensaje' => 'Login exitoso',
    'token'   => $token,
    'usuario' => [
        'id'     => $userId,
        'nombre' => $nombreFinal,
        'correo' => $correo
    ]
]);

$conn->close();
