$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$nginxRoot = Join-Path $projectRoot 'admin-dist\nginx-1.20.2'
$nginx = Join-Path $nginxRoot 'nginx.exe'

if (-not (Test-Path -LiteralPath $nginx)) {
    throw "Nginx executable not found: $nginx"
}

& (Join-Path $PSScriptRoot 'check-nginx.ps1')
Start-Process -FilePath $nginx -WorkingDirectory $nginxRoot -WindowStyle Hidden
Write-Host 'Nginx started on http://127.0.0.1/'

