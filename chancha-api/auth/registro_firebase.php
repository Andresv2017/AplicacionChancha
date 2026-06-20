<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$data          = json_decode(file_get_contents('php://input'), true);
$firebaseToken = trim($data['firebase_token'] ?? '');
$nombre        = trim($data['nombre'] ?? '');

if (!$firebaseToken || !$nombre) {
    http_response_code(400);
    echo json_encode(['error' => 'Token y nombre son obligatorios']);
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
    echo json_encode(['error' => 'Token invalido']);
    exit();
}

$correo = $tokenData['email'];

$conn = getConnection();

// Verificar que no exista ya
$stmt = $conn->prepare("SELECT id FROM usuarios WHERE correo = ?");
$stmt->bind_param("s", $correo);
$stmt->execute();
if ($stmt->get_result()->num_rows > 0) {
    http_response_code(409);
    echo json_encode(['error' => 'El correo ya esta registrado']);
    exit();
}

// Crear usuario (sin contrasena, Firebase la gestiona)
$stmt = $conn->prepare("INSERT INTO usuarios (nombre, correo, contrasena) VALUES (?, ?, '')");
$stmt->bind_param("ss", $nombre, $correo);

if ($stmt->execute()) {
    http_response_code(201);
    echo json_encode(['mensaje' => 'Usuario registrado correctamente']);
} else {
    http_response_code(500);
    echo json_encode(['error' => 'Error al registrar usuario']);
}

$conn->close();
