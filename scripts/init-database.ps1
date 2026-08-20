param(
    [string]$MySqlPath = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe',
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Username = 'root'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$sqlFile = Join-Path $projectRoot 'database\cloud_taste.sql'

if (-not (Test-Path -LiteralPath $MySqlPath)) {
    throw "MySQL client not found: $MySqlPath"
}
if (-not (Test-Path -LiteralPath $sqlFile)) {
    throw "Database script not found: $sqlFile"
}

$password = $env:CLOUD_TASTE_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($password)) {
    $securePassword = Read-Host 'Enter MySQL password (hidden)' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

$previousPassword = $env:MYSQL_PWD
$env:MYSQL_PWD = $password
try {
    $existing = & $MySqlPath --protocol=TCP -h $HostName -P $Port -u $Username --batch --skip-column-names `
        -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME='cloud_taste';"
    if ($LASTEXITCODE -ne 0) {
        throw 'MySQL login failed. Check the username, password, and service status.'
    }
    if ($existing -contains 'cloud_taste') {
        throw 'The cloud_taste database already exists. Import stopped to prevent data loss.'
    }

    $sourcePath = $sqlFile.Replace('\', '/')
    & $MySqlPath --protocol=TCP -h $HostName -P $Port -u $Username --default-character-set=utf8mb4 `
        -e "source $sourcePath"
    if ($LASTEXITCODE -ne 0) {
        throw 'Database import failed. Review the MySQL output.'
    }

    $tableCount = & $MySqlPath --protocol=TCP -h $HostName -P $Port -u $Username --batch --skip-column-names `
        -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='cloud_taste';"
    if ($tableCount -ne '11') {
        throw "Unexpected table count after import: $tableCount"
    }

    Write-Host 'cloud_taste database initialized with 11 tables.'
}
finally {
    $env:MYSQL_PWD = $previousPassword
    $password = $null
}
