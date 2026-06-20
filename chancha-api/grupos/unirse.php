<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data   = json_decode(file_get_contents('php://input'), true);
$codigo = trim(strtoupper($data['codigo_invitacion'] ?? ''));

if (!$codigo) {
    http_response_code(400);
    echo json_encode(['error' => 'Codigo de invitacion requerido']);
    exit();
}

$stmt = $conn->prepare("SELECT id, nombre FROM grupos WHERE codigo_invitacion = ?");
$stmt->bind_param("s", $codigo);
$stmt->execute();
$grupo = $stmt->get_result()->fetch_assoc();

if (!$grupo) {
    http_response_code(404);
    echo json_encode(['error' => 'Codigo de invitacion invalido']);
    exit();
}

// Verificar si ya es miembro
$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $grupo['id'], $userId);
$check->execute();
if ($check->get_result()->num_rows > 0) {
    http_response_code(409);
    echo json_encode(['error' => 'Ya eres miembro de este grupo']);
    exit();
}

$stmt2 = $conn->prepare("INSERT INTO grupo_miembros (grupo_id, usuario_id) VALUES (?, ?)");
$stmt2->bind_param("ii", $grupo['id'], $userId);
$stmt2->execute();

echo json_encode([
    'mensaje'  => 'Te uniste al grupo correctamente',
    'grupo_id' => $grupo['id'],
    'nombre'   => $grupo['nombre']
]);

$conn->close();
