<?php
require_once 'config/db.php';
$conn = getConnection();
echo json_encode([
    'estado'       => 'OK',
    'servidor'     => 'Chancha API corriendo',
    'db_conectada' => $conn->ping() ? 'SI' : 'NO',
    'db_nombre'    => DB_NAME,
    'db_puerto'    => DB_PORT
]);
$conn->close();
