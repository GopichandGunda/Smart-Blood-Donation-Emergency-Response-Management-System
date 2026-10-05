$ErrorActionPreference = "Stop"

$credentialPath = Join-Path $env:LOCALAPPDATA "BloodManagement\blood-app.dpapi"
if (-not (Test-Path $credentialPath)) {
    throw "Local database credentials were not found. Run .\scripts\setup-local-database.ps1 first."
}

$protectedValue = [System.IO.File]::ReadAllText($credentialPath).Trim()
$secureValue = ConvertTo-SecureString -String $protectedValue
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
try {
    $env:BLOOD_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    $env:BLOOD_DB_USER = "blood_app"
    if ([string]::IsNullOrWhiteSpace($env:BLOOD_DB_URL)) {
        $env:BLOOD_DB_URL = "jdbc:mysql://127.0.0.1:3306/emergency_blood_management?serverTimezone=UTC"
    }

    Push-Location (Join-Path $PSScriptRoot "..")
    try {
        & .\mvnw.cmd exec:java
        if ($LASTEXITCODE -ne 0) {
            throw "The desktop application exited with code $LASTEXITCODE."
        }
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:BLOOD_DB_PASSWORD,Env:BLOOD_DB_USER -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}
