$ErrorActionPreference = "Stop"

$mysqlHome = $env:MYSQL_HOME
if ([string]::IsNullOrWhiteSpace($mysqlHome)) {
    $mysqlHome = Join-Path $env:LOCALAPPDATA "Programs\MySQL-8.4.9\mysql-8.4.9-winx64"
}

$server = Join-Path $mysqlHome "bin\mysqld.exe"
if (-not (Test-Path $server)) {
    throw "MySQL Server was not found at '$server'. Set MYSQL_HOME to the extracted MySQL Server folder."
}

$dataDirectory = $env:MYSQL_DATA_DIR
if ([string]::IsNullOrWhiteSpace($dataDirectory)) {
    $dataDirectory = Join-Path $env:LOCALAPPDATA "MySQL\data"
}

if (-not (Test-Path (Join-Path $dataDirectory "auto.cnf"))) {
    New-Item -ItemType Directory -Force -Path $dataDirectory | Out-Null
    & $server "--basedir=$mysqlHome" "--datadir=$dataDirectory" --initialize-insecure --console
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL data directory initialization failed with exit code $LASTEXITCODE."
    }
}

$listener = Test-NetConnection -ComputerName "127.0.0.1" -Port 3306 -InformationLevel Quiet
if ($listener) {
    throw "A server is already listening on 127.0.0.1:3306."
}

& $server "--basedir=$mysqlHome" "--datadir=$dataDirectory" --bind-address=127.0.0.1 --port=3306 --mysqlx=0 --console
if ($LASTEXITCODE -ne 0) {
    throw "MySQL Server exited with code $LASTEXITCODE."
}
