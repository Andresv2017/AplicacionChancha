<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$data = json_decode(file_get_contents('php://input'), true);
$nombre  = trim($data['nombre']  ?? '');
$correo  = trim($data['correo']  ?? '');
$pass    = trim($data['contrasena'] ?? '');
$pass2   = trim($data['contrasena2'] ?? '');

// Validaciones
if (!$nombre || !$correo || !$pass || !$pass2) {
    http_response_code(400);
    echo json_encode(['error' => 'Todos los campos son obligatorios']);
    exit();
}

if (!preg_match('/^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.(edu\.pe|upn\.edu\.pe)$/i', $correo)) {
    http_response_code(400);
    echo json_encode(['error' => 'El correo debe ser institucional (.edu.pe)']);
    exit();
}

if ($pass !== $pass2) {
    http_response_code(400);
    echo json_encode(['error' => 'Las contrasenas no coinciden']);
    exit();
}

if (strlen($pass) < 6) {
    http_response_code(400);
    echo json_encode(['error' => 'La contrasena debe tener al menos 6 caracteres']);
    exit();
}

$conn = getConnection();

// Verificar correo duplicado
$stmt = $conn->prepare("SELECT id FROM usuarios WHERE correo = ?");
$stmt->bind_param("s", $correo);
$stmt->execute();
if ($stmt->get_result()->num_rows > 0) {
    http_response_code(409);
    echo json_encode(['error' => 'El correo ya esta registrado']);
    exit();
}

$hash = password_hash($pass, PASSWORD_BCRYPT);
$stmt = $conn->prepare("INSERT INTO usuarios (nombre, correo, contrasena) VALUES (?, ?, ?)");
$stmt->bind_param("sss", $nombre, $correo, $hash);

if ($stmt->execute()) {
    http_response_code(201);
    echo json_encode(['mensaje' => 'Usuario registrado correctamente']);
} else {
    http_response_code(500);
    echo json_encode(['error' => 'Error al registrar usuario']);
}

$conn->close();
