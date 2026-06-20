<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn    = getConnection();
$userId  = getUserIdFromToken($conn);
$grupoId = (int)($_GET['grupo_id'] ?? 0);

if (!$grupoId) {
    http_response_code(400);
    echo json_encode(['error' => 'grupo_id requerido']);
    exit();
}

// Verificar que el usuario pertenece al grupo
$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $grupoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

$stmt = $conn->prepare("
    SELECT u.id, u.nombre, u.correo, u.foto_perfil, gm.fecha_union,
           (u.id = g.creado_por_id) AS es_creador
    FROM grupo_miembros gm
    JOIN usuarios u ON u.id = gm.usuario_id
    JOIN grupos g   ON g.id = gm.grupo_id
    WHERE gm.grupo_id = ?
    ORDER BY es_creador DESC, u.nombre ASC
");
$stmt->bind_param("i", $grupoId);
$stmt->execute();
$miembros = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

echo json_encode([
    'miembros'       => $miembros,
    'usuario_actual' => $userId
]);

$conn->close();
