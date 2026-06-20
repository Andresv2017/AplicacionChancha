<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn    = getConnection();
$userId  = getUserIdFromToken($conn);
$gastoId = (int)($_GET['gasto_id'] ?? 0);

if (!$gastoId) {
    http_response_code(400);
    echo json_encode(['error' => 'gasto_id requerido']);
    exit();
}

// Obtener gasto y verificar membresía
$stmt = $conn->prepare("
    SELECT ga.*, u.nombre AS pagado_por_nombre
    FROM gastos ga
    JOIN usuarios u ON u.id = ga.pagado_por_id
    WHERE ga.id = ?
");
$stmt->bind_param("i", $gastoId);
$stmt->execute();
$gasto = $stmt->get_result()->fetch_assoc();

if (!$gasto) {
    http_response_code(404);
    echo json_encode(['error' => 'Gasto no encontrado']);
    exit();
}

$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $gasto['grupo_id'], $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

// Obtener partes con nombre de usuario
$stmtP = $conn->prepare("
    SELECT gp.usuario_id, gp.monto_asignado, gp.estado_pago, u.nombre
    FROM gasto_partes gp
    JOIN usuarios u ON u.id = gp.usuario_id
    WHERE gp.gasto_id = ?
    ORDER BY gp.usuario_id
");
$stmtP->bind_param("i", $gastoId);
$stmtP->execute();
$partes = $stmtP->get_result()->fetch_all(MYSQLI_ASSOC);

echo json_encode(['gasto' => $gasto, 'partes' => $partes]);
$conn->close();
