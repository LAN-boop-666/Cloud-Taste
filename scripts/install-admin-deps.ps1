$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendRoot = Join-Path $projectRoot 'admin-web\project-sky-admin-vue-ts'
$nodeRoot = Join-Path $projectRoot '.tools\node-v12.22.0\nodejs'
$node = Join-Path $nodeRoot 'node.exe'
$npmCli = Join-Path $projectRoot '.tools\npm-7.24.2\node_modules\npm\bin\npm-cli.js'
$lock = Join-Path $frontendRoot 'package-lock.json'
$backup = Join-Path $env:TEMP 'cloud-taste-package-lock.original.json'

foreach ($path in @($node, $npmCli, $lock)) {
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Required file is missing: $path"
    }
}

Copy-Item -LiteralPath $lock -Destination $backup -Force
$exitCode = 1
try {
    $raw = [System.IO.File]::ReadAllText($lock, [System.Text.Encoding]::UTF8)
    $fixed = [regex]::Replace($raw, 'https://registry\.npm\.taobao\.org/(?<package>@[^/]+/[^/]+|[^/]+)/download/(?<file>[^?" ]+)', {
        param($match)
        $packageName = $match.Groups['package'].Value
        $fileName = $match.Groups['file'].Value
        if ($packageName.StartsWith('@')) {
            $scope = $packageName.Split('/')[0] + '/'
            if ($fileName.StartsWith($scope)) {
                $fileName = $fileName.Substring($scope.Length)
            }
        }
        "https://registry.npmjs.org/$packageName/-/$fileName"
    })
    [System.IO.File]::WriteAllText($lock, $fixed, (New-Object System.Text.UTF8Encoding($false)))

    $env:Path = "$nodeRoot;$env:Path"
    $env:CYPRESS_INSTALL_BINARY = '0'
    Push-Location $frontendRoot
    try {
        & $node $npmCli ci --registry=https://registry.npmjs.org --no-audit --no-fund
        $exitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    Copy-Item -LiteralPath $backup -Destination $lock -Force
    if (Test-Path -LiteralPath $backup) {
        [System.IO.File]::Delete($backup)
    }
}

if ($exitCode -ne 0) {
    throw "Frontend dependency installation failed with exit code $exitCode. Original package-lock.json was restored."
}

Write-Host 'Frontend dependencies installed with project-local Node 12.22.0.'
Write-Host 'Original package-lock.json was restored after installation.'
