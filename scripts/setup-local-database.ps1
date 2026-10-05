$ErrorActionPreference = "Stop"

$mysqlHome = $env:MYSQL_HOME
if ([string]::IsNullOrWhiteSpace($mysqlHome)) {
    $mysqlHome = Join-Path $env:LOCALAPPDATA "Programs\MySQL-8.4.9\mysql-8.4.9-winx64"
}
$client = Join-Path $mysqlHome "bin\mysql.exe"
if (-not (Test-Path $client)) {
    throw "MySQL client was not found at '$client'. Set MYSQL_HOME to the extracted MySQL Server folder."
}
if (-not (Test-NetConnection -ComputerName "127.0.0.1" -Port 3306 -InformationLevel Quiet)) {
    throw "MySQL is not running. Start it first with .\scripts\start-local-mysql.ps1 in another terminal."
}

$credentialDirectory = Join-Path $env:LOCALAPPDATA "BloodManagement"
$rootCredentialPath = Join-Path $credentialDirectory "mysql-root.dpapi"
$appCredentialPath = Join-Path $credentialDirectory "blood-app.dpapi"
New-Item -ItemType Directory -Force -Path $credentialDirectory | Out-Null

function New-RandomPassword {
    $bytes = New-Object byte[] 32
    $generator = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
    try {
        $generator.GetBytes($bytes)
    } finally {
        $generator.Dispose()
    }
    try {
        return -join ($bytes | ForEach-Object { $_.ToString("x2") })
    } finally {
        [Array]::Clear($bytes, 0, $bytes.Length)
    }
}

function Save-CurrentUserSecret([string]$Path, [string]$Value) {
    $secureValue = ConvertTo-SecureString -String $Value -AsPlainText -Force
    $protectedValue = ConvertFrom-SecureString -SecureString $secureValue
    [System.IO.File]::WriteAllText($Path, $protectedValue)
}

function Read-CurrentUserSecret([string]$Path) {
    $protectedValue = [System.IO.File]::ReadAllText($Path).Trim()
    $secureValue = ConvertTo-SecureString -String $protectedValue
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$isFirstSetup = -not (Test-Path $rootCredentialPath)
if ($isFirstSetup) {
    $rootPassword = New-RandomPassword
} else {
    $rootPassword = Read-CurrentUserSecret $rootCredentialPath
}
$appPassword = New-RandomPassword
$sql = New-Object System.Text.StringBuilder
if ($isFirstSetup) {
    [void]$sql.AppendLine("ALTER USER 'root'@'localhost' IDENTIFIED BY '$rootPassword';")
}
[void]$sql.AppendLine("CREATE DATABASE IF NOT EXISTS emergency_blood_management CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;")
[void]$sql.AppendLine("CREATE USER IF NOT EXISTS 'blood_app'@'127.0.0.1' IDENTIFIED BY '$appPassword';")
[void]$sql.AppendLine("ALTER USER 'blood_app'@'127.0.0.1' IDENTIFIED BY '$appPassword';")
[void]$sql.AppendLine("GRANT SELECT, INSERT, UPDATE, DELETE ON emergency_blood_management.* TO 'blood_app'@'127.0.0.1';")
[void]$sql.AppendLine([System.IO.File]::ReadAllText((Join-Path $PSScriptRoot "..\src\main\resources\database.sql")))
[void]$sql.AppendLine([System.IO.File]::ReadAllText((Join-Path $PSScriptRoot "..\src\main\resources\sample-data.sql")))

$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = $client
$startInfo.Arguments = "--protocol=tcp --host=127.0.0.1 --port=3306 --user=root --default-character-set=utf8mb4"
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.EnvironmentVariables["MYSQL_PWD"] = $rootPassword
$process = New-Object System.Diagnostics.Process
$process.StartInfo = $startInfo
if (-not $process.Start()) {
    throw "Could not start the MySQL client."
}
$process.StandardInput.Write($sql.ToString())
$process.StandardInput.Close()
$output = $process.StandardOutput.ReadToEnd()
$errors = $process.StandardError.ReadToEnd()
$process.WaitForExit()
if ($process.ExitCode -ne 0) {
    throw "MySQL setup failed with exit code $($process.ExitCode). $errors"
}

if ($isFirstSetup) {
    Save-CurrentUserSecret $rootCredentialPath $rootPassword
}
Save-CurrentUserSecret $appCredentialPath $appPassword
[Array]::Clear([char[]]$rootPassword, 0, $rootPassword.Length)
[Array]::Clear([char[]]$appPassword, 0, $appPassword.Length)
[void]$sql.Clear()

Write-Output "Database schema and sample data are ready."
Write-Output "A least-privilege blood_app account was created."
Write-Output "Database credentials are protected for this Windows account with DPAPI and were not written to the repository."
