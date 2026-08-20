param(
    [int]$Port = 8889
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $projectRoot 'admin-web\project-sky-admin-vue-ts'
$nodeRoot = Join-Path $projectRoot '.tools\node-v12.22.0\nodejs'
$npm = Join-Path $nodeRoot 'npm.cmd'

if (-not (Test-Path -LiteralPath $npm)) {
    throw "Project-local Node 12 is missing: $nodeRoot"
}

if (-not (Test-Path -LiteralPath (Join-Path $frontendRoot 'node_modules\@vue\cli-service\bin\vue-cli-service.js'))) {
    throw 'Frontend dependencies are missing. Run the install command in the environment guide first.'
}

$env:Path = "$nodeRoot;$env:Path"
Push-Location $frontendRoot
try {
    Write-Host "Using Node: $(& (Join-Path $nodeRoot 'node.exe') --version)"
    Write-Host "Frontend directory: $frontendRoot"
    Write-Host "Frontend URL: http://127.0.0.1:$Port/"
    & $npm run serve -- --port $Port
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend dev server exited with code $LASTEXITCODE."
    }
}
finally {
    Pop-Location
}
