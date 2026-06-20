<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$colectaId = (int)($_GET['colecta_id'] ?? 0);
$codigoQr  = trim($_GET['codigo_qr']   ?? '');

if (!$colectaId && !$codigoQr) {
    http_response_code(400);
    echo json_encode(['error' => 'colecta_id o codigo_qr requerido']);
    exit();
}

// Buscar la colecta
if ($colectaId) {
    $stmt = $conn->prepare("SELECT * FROM colectas WHERE id = ?");
    $stmt->bind_param("i", $colectaId);
} else {
    $stmt = $conn->prepare("SELECT * FROM colectas WHERE codigo_qr = ?");
    $stmt->bind_param("s", $codigoQr);
}
$stmt->execute();
$colecta = $stmt->get_result()->fetch_assoc();

if (!$colecta) {
    http_response_code(404);
    echo json_encode(['error' => 'Colecta no encontrada']);
    exit();
}

// Verificar que el usuario pertenece al grupo
$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $colecta['grupo_id'], $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

// Obtener todas las partes con nombre de usuario
$stmtPartes = $conn->prepare("
    SELECT cp.id, cp.usuario_id, u.nombre, cp.estado_pago, cp.metodo_pago, cp.fecha_pago, cp.comprobante
    FROM colecta_partes cp
    JOIN usuarios u ON u.id = cp.usuario_id
    WHERE cp.colecta_id = ?
    ORDER BY
        FIELD(cp.estado_pago, 'pendiente', 'en_revision', 'pagado', 'confirmado'),
        u.nombre ASC
");
$stmtPartes->bind_param("i", $colecta['id']);
$stmtPartes->execute();
$resultPartes = $stmtPartes->get_result();

$partes = [];
while ($row = $resultPartes->fetch_assoc()) {
    $partes[] = $row;
}

echo json_encode([
    'colecta' => $colecta,
    'partes'  => $partes
]);

$conn->close();
