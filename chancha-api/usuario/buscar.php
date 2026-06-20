<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn    = getConnection();
$userId  = getUserIdFromToken($conn);
$correo  = trim($_GET['correo']   ?? '');
$grupoId = (int)($_GET['grupo_id'] ?? 0);

if (!$correo) {
    http_response_code(400);
    echo json_encode(['error' => 'Correo requerido']);
    exit();
}

$stmt = $conn->prepare("SELECT id, nombre, correo FROM usuarios WHERE correo = ? AND id != ?");
$stmt->bind_param("si", $correo, $userId);
$stmt->execute();
$usuario = $stmt->get_result()->fetch_assoc();

if (!$usuario) {
    http_response_code(404);
    echo json_encode(['error' => 'Usuario no encontrado']);
    exit();
}

// Verificar si ya esta en el grupo
if ($grupoId) {
    $check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
    $check->bind_param("ii", $grupoId, $usuario['id']);
    $check->execute();
    if ($check->get_result()->num_rows > 0) {
        http_response_code(409);
        echo json_encode(['error' => 'Este usuario ya es miembro del grupo']);
        exit();
    }
}

echo json_encode(['usuario' => $usuario]);
$conn->close();
