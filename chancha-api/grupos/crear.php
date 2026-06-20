<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data       = json_decode(file_get_contents('php://input'), true);
$nombre     = trim($data['nombre']      ?? '');
$descripcion = trim($data['descripcion'] ?? '');

if (!$nombre) {
    http_response_code(400);
    echo json_encode(['error' => 'El nombre del grupo es obligatorio']);
    exit();
}

// Generar codigo de invitacion unico
do {
    $codigo = strtoupper(bin2hex(random_bytes(4))); // 8 chars, ej: "A3F2B901"
    $check  = $conn->prepare("SELECT id FROM grupos WHERE codigo_invitacion = ?");
    $check->bind_param("s", $codigo);
    $check->execute();
} while ($check->get_result()->num_rows > 0);

$conn->begin_transaction();
try {
    $stmt = $conn->prepare(
        "INSERT INTO grupos (nombre, descripcion, codigo_invitacion, creado_por_id) VALUES (?, ?, ?, ?)"
    );
    $stmt->bind_param("sssi", $nombre, $descripcion, $codigo, $userId);
    $stmt->execute();
    $grupoId = $conn->insert_id;

    // El creador entra automaticamente como miembro
    $stmt2 = $conn->prepare("INSERT INTO grupo_miembros (grupo_id, usuario_id) VALUES (?, ?)");
    $stmt2->bind_param("ii", $grupoId, $userId);
    $stmt2->execute();

    $conn->commit();

    http_response_code(201);
    echo json_encode([
        'mensaje'           => 'Grupo creado',
        'grupo_id'          => $grupoId,
        'codigo_invitacion' => $codigo
    ]);
} catch (Exception $e) {
    $conn->rollback();
    http_response_code(500);
    echo json_encode(['error' => 'Error al crear grupo']);
}

$conn->close();
