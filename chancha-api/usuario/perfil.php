<?php
require_once '../config/db.php';

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

if ($_SERVER['REQUEST_METHOD'] === 'GET') {

    $stmt = $conn->prepare("
        SELECT u.id, u.nombre, u.correo, u.foto_perfil, u.fecha_registro,
               (SELECT COUNT(*) FROM grupo_miembros gm WHERE gm.usuario_id = u.id) AS grupos_activos,
               (SELECT COUNT(*) FROM gasto_partes gp
                JOIN gastos ga ON ga.id = gp.gasto_id
                WHERE gp.usuario_id = u.id AND gp.estado_pago = 'pendiente'
                  AND ga.pagado_por_id != u.id) AS deudas_pendientes,
               (SELECT COALESCE(SUM(gp.monto_asignado),0) FROM gasto_partes gp
                JOIN gastos ga ON ga.id = gp.gasto_id
                WHERE gp.usuario_id = u.id AND gp.estado_pago = 'pendiente'
                  AND ga.pagado_por_id != u.id) AS monto_total_debo,
               (SELECT COALESCE(SUM(gp.monto_asignado),0) FROM gasto_partes gp
                JOIN gastos ga ON ga.id = gp.gasto_id
                WHERE ga.pagado_por_id = u.id AND gp.usuario_id != u.id
                  AND gp.estado_pago = 'pendiente') AS monto_total_me_deben
        FROM usuarios u WHERE u.id = ?
    ");
    $stmt->bind_param("i", $userId);
    $stmt->execute();
    $perfil = $stmt->get_result()->fetch_assoc();

    echo json_encode(['perfil' => $perfil]);

} elseif ($_SERVER['REQUEST_METHOD'] === 'PUT') {

    $data   = json_decode(file_get_contents('php://input'), true);
    $nombre = trim($data['nombre'] ?? '');

    if (!$nombre) {
        http_response_code(400);
        echo json_encode(['error' => 'El nombre no puede estar vacio']);
        exit();
    }

    $stmt = $conn->prepare("UPDATE usuarios SET nombre = ? WHERE id = ?");
    $stmt->bind_param("si", $nombre, $userId);
    $stmt->execute();
    echo json_encode(['mensaje' => 'Perfil actualizado']);

} else {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
}

$conn->close();
