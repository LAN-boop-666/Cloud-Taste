$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$nginxRoot = Join-Path $projectRoot 'admin-dist\nginx-1.20.2'
$nginx = Join-Path $nginxRoot 'nginx.exe'

if (-not (Test-Path -LiteralPath $nginx)) {
    throw "Nginx executable not found: $nginx"
}

& $nginx -t -p $nginxRoot -c conf/nginx.conf
if ($LASTEXITCODE -ne 0) {
    throw "Nginx configuration check failed with exit code $LASTEXITCODE."
}

Write-Host 'Nginx configuration is valid.'

