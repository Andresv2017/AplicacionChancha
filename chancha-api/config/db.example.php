<?php
// Copia este archivo como db.php y ajusta los valores a tu entorno local
ini_set('display_errors', 0);
error_reporting(E_ALL);

define('DB_HOST', 'localhost');
define('DB_PORT', 3306);        // XAMPP por defecto usa 3306; algunos usan 3307
define('DB_NAME', 'chancha_db');
define('DB_USER', 'root');
define('DB_PASS', '');          // Vacío por defecto en XAMPP

function getConnection() {
    $conn = new mysqli(DB_HOST, DB_USER, DB_PASS, DB_NAME, DB_PORT);
    if ($conn->connect_error) {
        http_response_code(500);
        die(json_encode(['error' => 'Error de conexion a la base de datos']));
    }
    $conn->set_charset('utf8mb4');
    return $conn;
}

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

function getTokenFromHeader() {
    $headers = getallheaders();
    if (isset($headers['Authorization'])) {
        $parts = explode(' ', $headers['Authorization']);
        if (count($parts) === 2 && $parts[0] === 'Bearer') {
            return $parts[1];
        }
    }
    return null;
}

function getUserIdFromToken($conn) {
    $token = getTokenFromHeader();
    if (!$token) {
        http_response_code(401);
        die(json_encode(['error' => 'Token requerido']));
    }
    $stmt = $conn->prepare("SELECT id FROM usuarios WHERE token_sesion = ?");
    $stmt->bind_param("s", $token);
    $stmt->execute();
    $result = $stmt->get_result();
    $user = $result->fetch_assoc();
    if (!$user) {
        http_response_code(401);
        die(json_encode(['error' => 'Token invalido o expirado']));
    }
    return $user['id'];
}
