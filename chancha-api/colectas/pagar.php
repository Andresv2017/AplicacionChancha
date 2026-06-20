<?php
require_once '../config/db.php';

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['error' => 'Metodo no permitido']);
    exit();
}

$conn   = getConnection();
$userId = getUserIdFromToken($conn);

$colectaId = (int)($_POST['colecta_id'] ?? 0);
$file      = $_FILES['comprobante'] ?? null;

if (!$colectaId || !$file || $file['error'] !== UPLOAD_ERR_OK) {
    http_response_code(400);
    echo json_encode(['error' => 'Datos incompletos o error al subir archivo']);
    exit();
}

// Validar tipo de archivo
$allowedTypes = ['image/jpeg', 'image/png', 'image/jpg', 'image/webp'];
$finfo        = finfo_open(FILEINFO_MIME_TYPE);
$mimeType     = finfo_file($finfo, $file['tmp_name']);
finfo_close($finfo);

if (!in_array($mimeType, $allowedTypes)) {
    http_response_code(400);
    echo json_encode(['error' => 'Solo se permiten imagenes (JPG, PNG, WEBP)']);
    exit();
}

// Verificar que la parte existe y está pendiente
$stmtCheck = $conn->prepare(
    "SELECT id, estado_pago FROM colecta_partes WHERE colecta_id = ? AND usuario_id = ?"
);
$stmtCheck->bind_param("ii", $colectaId, $userId);
$stmtCheck->execute();
$parte = $stmtCheck->get_result()->fetch_assoc();

if (!$parte) {
    http_response_code(403);
    echo json_encode(['error' => 'No participas en esta colecta']);
    exit();
}

if ($parte['estado_pago'] !== 'pendiente') {
    http_response_code(409);
    echo json_encode(['error' => 'Ya enviaste un comprobante o tu pago fue confirmado']);
    exit();
}

// Guardar imagen
$uploadDir = __DIR__ . '/../uploads/comprobantes/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

$ext      = ($mimeType === 'image/png') ? 'png' : (($mimeType === 'image/webp') ? 'webp' : 'jpg');
$filename = 'colecta_' . uniqid() . '.' . $ext;
$filepath = $uploadDir . $filename;

if (!move_uploaded_file($file['tmp_name'], $filepath)) {
    http_response_code(500);
    echo json_encode(['error' => 'Error al guardar la imagen']);
    exit();
}

$relativePath = 'uploads/comprobantes/' . $filename;
$now          = date('Y-m-d H:i:s');

$stmt = $conn->prepare(
    "UPDATE colecta_partes SET estado_pago = 'en_revision', comprobante = ?, fecha_pago = ?
     WHERE id = ?"
);
$stmt->bind_param("ssi", $relativePath, $now, $parte['id']);
$stmt->execute();

// Notificar al creador de la colecta
$stmtColecta = $conn->prepare("SELECT creador_id, descripcion FROM colectas WHERE id = ?");
$stmtColecta->bind_param("i", $colectaId);
$stmtColecta->execute();
$colecta = $stmtColecta->get_result()->fetch_assoc();

if ($colecta && $colecta['creador_id'] != $userId) {
    $stmtNombre = $conn->prepare("SELECT nombre FROM usuarios WHERE id = ?");
    $stmtNombre->bind_param("i", $userId);
    $stmtNombre->execute();
    $usuario = $stmtNombre->get_result()->fetch_assoc();
    $nombre  = $usuario ? $usuario['nombre'] : 'Alguien';

    $msg  = "$nombre envió comprobante en: " . $colecta['descripcion'] . ". Revísalo para confirmar.";
    $tipo = 'pago_colecta';
    $notif = $conn->prepare("INSERT INTO notificaciones (usuario_id, tipo, mensaje) VALUES (?, ?, ?)");
    $notif->bind_param("iss", $colecta['creador_id'], $tipo, $msg);
    $notif->execute();
}

echo json_encode(['mensaje' => 'Comprobante enviado. El admin debe confirmarlo.']);
$conn->close();
