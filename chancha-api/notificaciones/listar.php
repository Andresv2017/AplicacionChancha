<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$stmt = $conn->prepare("
    SELECT id, tipo, mensaje, leida, fecha
    FROM notificaciones
    WHERE usuario_id = ?
    ORDER BY fecha DESC
    LIMIT 50
");
$stmt->bind_param("i", $userId);
$stmt->execute();
$notifs = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

$noLeidas = count(array_filter($notifs, fn($n) => !$n['leida']));

echo json_encode([
    'notificaciones' => $notifs,
    'no_leidas'      => $noLeidas
]);

$conn->close();
