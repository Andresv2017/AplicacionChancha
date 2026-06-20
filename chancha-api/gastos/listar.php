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

// Verificar que pertenece al grupo
$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $grupoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

$sql = "
    SELECT
        g.id,
        g.descripcion,
        g.monto_total,
        g.tipo_division,
        g.fecha,
        g.pagado_por_id,
        u.nombre          AS pagado_por_nombre,
        gp.monto_asignado AS mi_parte,
        gp.estado_pago    AS mi_estado
    FROM gastos g
    JOIN usuarios u ON u.id = g.pagado_por_id
    LEFT JOIN gasto_partes gp ON gp.gasto_id = g.id AND gp.usuario_id = ?
    WHERE g.grupo_id = ?
    ORDER BY g.fecha DESC, g.creado_en DESC
";

$stmt = $conn->prepare($sql);
$stmt->bind_param("ii", $userId, $grupoId);
$stmt->execute();
$gastos = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

echo json_encode(['gastos' => $gastos]);
$conn->close();
