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

$check = $conn->prepare("SELECT id FROM grupo_miembros WHERE grupo_id = ? AND usuario_id = ?");
$check->bind_param("ii", $grupoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(403);
    echo json_encode(['error' => 'No perteneces a este grupo']);
    exit();
}

// Cada deuda individual con descripcion del gasto
$sql = "
    SELECT
        ga.id            AS gasto_id,
        ga.descripcion,
        ga.fecha,
        ga.monto_total,
        gp.monto_asignado AS monto,
        gp.usuario_id     AS deudor_id,
        ud.nombre         AS deudor,
        ga.pagado_por_id  AS acreedor_id,
        ua.nombre         AS acreedor,
        gp.estado_pago,
        gp.comprobante,
        (gp.usuario_id   = ?)  AS es_mi_deuda,
        (ga.pagado_por_id = ?) AS me_deben
    FROM gasto_partes gp
    JOIN gastos ga  ON ga.id  = gp.gasto_id
    JOIN usuarios ud ON ud.id = gp.usuario_id
    JOIN usuarios ua ON ua.id = ga.pagado_por_id
    WHERE ga.grupo_id    = ?
      AND gp.estado_pago IN ('pendiente', 'en_revision')
      AND gp.usuario_id != ga.pagado_por_id
    ORDER BY ga.fecha DESC, ga.creado_en DESC
";

$stmt = $conn->prepare($sql);
$stmt->bind_param("iii", $userId, $userId, $grupoId);
$stmt->execute();
$rows = $stmt->get_result()->fetch_all(MYSQLI_ASSOC);

// Convertir flags a booleanos
foreach ($rows as &$r) {
    $r['es_mi_deuda'] = (bool)$r['es_mi_deuda'];
    $r['me_deben']    = (bool)$r['me_deben'];
    $r['monto']       = (float)$r['monto'];
    $r['monto_total'] = (float)$r['monto_total'];
}

echo json_encode(['balances' => $rows]);
$conn->close();
