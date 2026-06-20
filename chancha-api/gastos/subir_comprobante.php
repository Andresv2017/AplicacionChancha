<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$gastoId = (int)($_POST['gasto_id'] ?? 0);
$file    = $_FILES['comprobante'] ?? null;

if (!$gastoId || !$file || $file['error'] !== UPLOAD_ERR_OK) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos o error al subir archivo']);
    exit();
}

// Validar tipo de archivo
$allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'];
$finfo = finfo_open(FILEINFO_MIME_TYPE);
$mimeType = finfo_file($finfo, $file['tmp_name']);
finfo_close($finfo);

if (!in_array($mimeType, $allowedTypes)) {
    http_response_code(400);
    echo json_encode(['error' => 'Solo se permiten imagenes (JPG, PNG, WEBP)']);
    exit();
}

// Verificar que el usuario es el deudor y el pago está pendiente
$check = $conn->prepare("
    SELECT gp.id FROM gasto_partes gp
    JOIN gastos g ON g.id = gp.gasto_id
    WHERE gp.gasto_id   = ?
      AND gp.usuario_id = ?
      AND gp.estado_pago = 'pendiente'
      AND gp.usuario_id != g.pagado_por_id
");
$check->bind_param("ii", $gastoId, $userId);
$check->execute();
if ($check->get_result()->num_rows === 0) {
    http_response_code(404);
    echo json_encode(['error' => 'Deuda no encontrada o ya no está pendiente']);
    exit();
}

// Guardar imagen
$uploadDir = __DIR__ . '/../uploads/comprobantes/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

$ext      = ($mimeType === 'image/png') ? 'png' : (($mimeType === 'image/webp') ? 'webp' : 'jpg');
$filename = 'pago_' . uniqid() . '.' . $ext;
$filepath = $uploadDir . $filename;

if (!move_uploaded_file($file['tmp_name'], $filepath)) {
    http_response_code(500);
    echo json_encode(['error' => 'Error al guardar la imagen']);
    exit();
}

$relativePath = 'uploads/comprobantes/' . $filename;

// Actualizar estado a en_revision
$stmt = $conn->prepare("
    UPDATE gasto_partes SET estado_pago = 'en_revision', comprobante = ?
    WHERE gasto_id = ? AND usuario_id = ?
");
$stmt->bind_param("sii", $relativePath, $gastoId, $userId);
$stmt->execute();

if ($stmt->affected_rows > 0) {
    echo json_encode(['mensaje' => 'Comprobante enviado', 'comprobante' => $relativePath]);
} else {
    http_response_code(500);
    echo json_encode(['error' => 'No se pudo actualizar el estado']);
}

$conn->close();
