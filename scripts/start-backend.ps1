param(
    [string]$DatabaseUsername = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $projectRoot 'backend\sky-take-out\sky-server\target\sky-server-1.0-SNAPSHOT.jar'

if (-not (Test-Path -LiteralPath $jar)) {
    & (Join-Path $PSScriptRoot 'build-backend.ps1')
}

$localConfig = Join-Path $projectRoot 'backend\sky-take-out\sky-server\src\main\resources\application-local.yml'
$localPasswordConfigured = $false
if (Test-Path -LiteralPath $localConfig) {
    $localConfigText = [System.IO.File]::ReadAllText($localConfig, [System.Text.Encoding]::UTF8)
    $passwordLine = [regex]::Match($localConfigText, '(?m)^\s*password:\s*(?<value>[^#\r\n]*)').Groups['value'].Value.Trim()
    $localPasswordConfigured = -not [string]::IsNullOrWhiteSpace($passwordLine) -and $passwordLine -notin @('""', "''")
    if ($localPasswordConfigured -and [string]::IsNullOrWhiteSpace($env:CLOUD_TASTE_DB_PASSWORD)) {
        $localPassword = $passwordLine
        if (($localPassword.StartsWith('"') -and $localPassword.EndsWith('"')) -or
            ($localPassword.StartsWith("'") -and $localPassword.EndsWith("'"))) {
            $localPassword = $localPassword.Substring(1, $localPassword.Length - 2)
        }
        $env:CLOUD_TASTE_DB_PASSWORD = $localPassword
    }
}

if ([string]::IsNullOrWhiteSpace($env:CLOUD_TASTE_DB_PASSWORD) -and -not $localPasswordConfigured) {
    $securePassword = Read-Host 'Enter MySQL password (hidden)' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $env:CLOUD_TASTE_DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

if (-not [string]::IsNullOrWhiteSpace($DatabaseUsername)) {
    $env:CLOUD_TASTE_DB_USERNAME = $DatabaseUsername
}

# The course project resolves the Druid password through a nested placeholder.
# Set the direct Spring property as well so local credentials work consistently.
if (-not [string]::IsNullOrWhiteSpace($env:CLOUD_TASTE_DB_PASSWORD)) {
    $env:SPRING_DATASOURCE_DRUID_PASSWORD = $env:CLOUD_TASTE_DB_PASSWORD
}
& java -jar $jar
