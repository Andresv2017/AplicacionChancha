<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data          = json_decode(file_get_contents('php://input'), true);
$grupoId       = (int)($data['grupo_id']        ?? 0);
$nuevoUsuarioId = (int)($data['nuevo_usuario_id'] ?? 0);

if (!$grupoId || !$nuevoUsuarioId) {
    http_response_code(400);
    echo json_encode(['error' => 'grupo_id y nuevo_usuario_id son requeridos']);
    exit();
}

// Solo miembros del grupo pueden agregar
$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $grupoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

// Verificar que el nuevo usuario existe
$checkU = $conn->prepare("SELECT id, nombre FROM usuarios WHERE id = ?");
$checkU->bind_param("i", $nuevoUsuarioId);
$checkU->execute();
$nuevoUsuario = $checkU->get_result()->fetch_assoc();
if (!$nuevoUsuario) {
    http_response_code(404);
    echo json_encode(['error' => 'Usuario no encontrado']);
    exit();
}

// Verificar que no sea ya miembro
$checkM = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$checkM->bind_param("ii", $grupoId, $nuevoUsuarioId);
$checkM->execute();
if ($checkM->get_result()->num_rows > 0) {
    http_response_code(409);
    echo json_encode(['error' => 'El usuario ya es miembro del grupo']);
    exit();
}

$stmt = $conn->prepare("INSERT INTO grupo_miembros (grupo_id, usuario_id) VALUES (?, ?)");
$stmt->bind_param("ii", $grupoId, $nuevoUsuarioId);
$stmt->execute();

// Notificar al nuevo miembro
$msg   = "Te agregaron al grupo";
$tipo  = 'grupo_nuevo';
$notif = $conn->prepare("INSERT INTO notificaciones (usuario_id, tipo, mensaje) VALUES (?, ?, ?)");
$notif->bind_param("iss", $nuevoUsuarioId, $tipo, $msg);
$notif->execute();

echo json_encode([
    'mensaje' => $nuevoUsuario['nombre'] . ' agregado al grupo'
]);
$conn->close();
