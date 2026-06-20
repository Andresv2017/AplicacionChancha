<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$data    = json_decode(file_get_contents('php://input'), true);
$gastoId = (int)($data['gasto_id'] ?? 0);

if (!$gastoId) {
    http_response_code(400);
    echo json_encode(['error' => 'gasto_id requerido']);
    exit();
}

// Obtener grupo del gasto
$stmt = $conn->prepare("SELECT grupo_id FROM gastos WHERE id = ?");
$stmt->bind_param("i", $gastoId);
$stmt->execute();
$row = $stmt->get_result()->fetch_assoc();

if (!$row) {
    http_response_code(404);
    echo json_encode(['error' => 'Gasto no encontrado']);
    exit();
}

// Solo el admin (creador del grupo) puede eliminar
$stmtAdmin = $conn->prepare("SELECT creado_por_id FROM grupos WHERE id = ?");
$stmtAdmin->bind_param("i", $row['grupo_id']);
$stmtAdmin->execute();
$grupo = $stmtAdmin->get_result()->fetch_assoc();

if (!$grupo || $grupo['creado_por_id'] != $userId) {
    http_response_code(403);
    echo json_encode(['error' => 'Solo el admin puede eliminar gastos']);
    exit();
}

// Eliminar (gasto_partes se borra en cascada)
$del = $conn->prepare("DELETE FROM gastos WHERE id = ?");
$del->bind_param("i", $gastoId);
$del->execute();

echo json_encode(['mensaje' => 'Gasto eliminado']);
$conn->close();
