<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'GET') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

// Grupos del usuario + cuanto debe o le deben en cada uno
$sql = "
    SELECT
        g.id,
        g.nombre,
        g.descripcion,
        g.codigo_invitacion,
        g.fecha_creacion,
        (SELECT COUNT(*) FROM grupo_miembros gm2 WHERE gm2.grupo_id = g.id) AS num_miembros,
        COALESCE((
            SELECT SUM(gp.monto_asignado)
            FROM gasto_partes gp
            JOIN gastos ga ON ga.id = gp.gasto_id
            WHERE gp.usuario_id = ?
              AND ga.grupo_id   = g.id
              AND gp.estado_pago = 'pendiente'
              AND ga.pagado_por_id != ?
        ), 0) AS total_debo,
        COALESCE((
            SELECT SUM(gp.monto_asignado)
            FROM gasto_partes gp
            JOIN gastos ga ON ga.id = gp.gasto_id
            WHERE ga.pagado_por_id = ?
              AND ga.grupo_id = g.id
              AND gp.usuario_id != ?
              AND gp.estado_pago = 'pendiente'
        ), 0) AS total_me_deben
    FROM grupos g
    JOIN grupo_miembros gm ON gm.grupo_id = g.id
    WHERE gm.usuario_id = ?
    ORDER BY g.fecha_creacion DESC
";

$stmt = $conn->prepare($sql);
$stmt->bind_param("iiiii", $userId, $userId, $userId, $userId, $userId);
$stmt->execute();
$grupos = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

echo json_encode(['grupos' => $grupos]);
$conn->close();
