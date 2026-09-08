[CmdletBinding()]
param(
    [switch]$ConfirmRecreate
)

$ErrorActionPreference = 'Stop'
if (-not $ConfirmRecreate) {
    throw 'This recreates only cloud_taste_loadtest. Run again with -ConfirmRecreate.'
}

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$localConfig = Join-Path $projectRoot 'backend\sky-take-out\sky-server\src\main\resources\application-local.yml'
$sourceSchema = Join-Path $projectRoot 'database\cloud_taste.sql'
$dataScript = Join-Path $PSScriptRoot 'loadtest-data.sql'
$temporarySchema = Join-Path $projectRoot '.tools\loadtest-schema.sql'
$mysql = 'D:\MySQL\MySQL Server 8.0\bin\mysql.exe'

if (-not (Test-Path -LiteralPath $mysql)) {
    $mysqlCommand = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if (-not $mysqlCommand) {
        throw 'mysql.exe was not found.'
    }
    $mysql = $mysqlCommand.Source
}

$raw = [IO.File]::ReadAllText($localConfig)
$passwordMatch = [regex]::Match($raw, '(?m)^\s*password\s*:\s*(?<value>.+?)\s*$')
if (-not $passwordMatch.Success) {
    throw 'The local MySQL password field was not found.'
}
$password = $passwordMatch.Groups['value'].Value.Trim().Trim('"').Trim("'")
if ([string]::IsNullOrWhiteSpace($password)) {
    throw 'The local MySQL password is empty.'
}

function Invoke-MySqlText([string]$sql) {
    $processInfo = [Diagnostics.ProcessStartInfo]::new()
    $processInfo.FileName = $mysql
    foreach ($argument in @('--protocol=tcp', '--host=127.0.0.1', '--port=3306', '--user=root', '--default-character-set=utf8mb4', '--batch')) {
        $processInfo.ArgumentList.Add($argument)
    }
    $processInfo.RedirectStandardInput = $true
    $processInfo.RedirectStandardOutput = $true
    $processInfo.RedirectStandardError = $true
    $processInfo.UseShellExecute = $false
    $processInfo.Environment['MYSQL_PWD'] = $password
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $processInfo
    [void]$process.Start()
    # 通过标准输入写入 UTF-8 字节，避免 Windows 默认代码页破坏中文 SQL 数据。
    $bytes = [Text.Encoding]::UTF8.GetBytes($sql)
    $process.StandardInput.BaseStream.Write($bytes, 0, $bytes.Length)
    $process.StandardInput.BaseStream.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "MySQL failed: $stderr"
    }
    return $stdout
}

New-Item -ItemType Directory -Force -Path (Split-Path $temporarySchema) | Out-Null
$schema = [IO.File]::ReadAllText($sourceSchema, [Text.Encoding]::UTF8)
$schema = $schema.Replace('USE `cloud_taste`;', 'USE `cloud_taste_loadtest`;')
[IO.File]::WriteAllText($temporarySchema, $schema, [Text.UTF8Encoding]::new($false))

try {
    [void](Invoke-MySqlText 'DROP DATABASE IF EXISTS cloud_taste_loadtest; CREATE DATABASE cloud_taste_loadtest CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;')
    [void](Invoke-MySqlText ([IO.File]::ReadAllText($temporarySchema, [Text.Encoding]::UTF8)))
    [void](Invoke-MySqlText ([IO.File]::ReadAllText($dataScript, [Text.Encoding]::UTF8)))
}
finally {
    if (Test-Path -LiteralPath $temporarySchema) {
        [IO.File]::Delete($temporarySchema)
    }
}

# 子脚本抛出的异常会被 ErrorActionPreference 捕获；PowerShell 脚本本身没有可靠的
# 原生进程退出码，不能用上一次 mysql 命令留下的 $LASTEXITCODE 判断这里是否成功。
& (Join-Path $PSScriptRoot 'generate-loadtest-users.ps1')

$counts = Invoke-MySqlText @'
USE cloud_taste_loadtest;
SELECT 'users', COUNT(*) FROM user WHERE id >= 10001;
SELECT 'addresses', COUNT(*) FROM address_book WHERE id >= 10001;
SELECT 'shopping_carts', COUNT(*) FROM shopping_cart WHERE id >= 30001;
SELECT 'orders', COUNT(*) FROM orders WHERE id >= 1000001;
SELECT 'order_details', COUNT(*) FROM order_detail WHERE id >= 2000001;
'@
Write-Host 'cloud_taste_loadtest is ready:'
Write-Host $counts.Trim()
