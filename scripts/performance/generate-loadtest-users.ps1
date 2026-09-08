[CmdletBinding()]
param(
    [ValidateRange(1, 10000)]
    [int]$Count = 1000,
    [int]$StartUserId = 10001,
    [ValidateRange(1, 24)]
    [int]$HoursValid = 2
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$configPath = Join-Path $projectRoot 'backend\sky-take-out\sky-server\src\main\resources\application.yml'
$outputDir = Join-Path $projectRoot '.tools'
$outputPath = Join-Path $outputDir 'loadtest-users.csv'

function Resolve-SpringValue([string]$value) {
    $trimmed = $value.Trim().Trim('"').Trim("'")
    $placeholder = [regex]::Match($trimmed, '^\$\{(?<name>[^:}]+)(:(?<default>.*))?\}$')
    if (-not $placeholder.Success) {
        return $trimmed
    }

    $environmentValue = [Environment]::GetEnvironmentVariable($placeholder.Groups['name'].Value)
    if (-not [string]::IsNullOrWhiteSpace($environmentValue)) {
        return $environmentValue
    }
    return $placeholder.Groups['default'].Value
}

function ConvertTo-Base64Url([byte[]]$bytes) {
    return [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+', '-').Replace('/', '_')
}

$raw = [IO.File]::ReadAllText($configPath)
$secretMatch = [regex]::Match($raw, '(?m)^\s*user-secret-key\s*:\s*(?<value>.+?)\s*$')
if (-not $secretMatch.Success) {
    throw 'user-secret-key was not found in application.yml.'
}

$secret = Resolve-SpringValue $secretMatch.Groups['value'].Value
if ([string]::IsNullOrWhiteSpace($secret)) {
    throw 'The user JWT secret is empty. No token file was generated.'
}

New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
$header = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}'))
$expiresAt = [DateTimeOffset]::UtcNow.AddHours($HoursValid)
$builder = [Text.StringBuilder]::new()
[void]$builder.AppendLine('userId,addressBookId,token')

for ($index = 0; $index -lt $Count; $index++) {
    $userId = $StartUserId + $index
    $payloadJson = '{"userId":' + $userId + ',"exp":' + $expiresAt.ToUnixTimeSeconds() + '}'
    $payload = ConvertTo-Base64Url ([Text.Encoding]::UTF8.GetBytes($payloadJson))
    $signingInput = $header + '.' + $payload
    $hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secret))
    try {
        $signature = ConvertTo-Base64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($signingInput)))
    }
    finally {
        $hmac.Dispose()
    }
    [void]$builder.AppendLine(('{0},{0},{1}.{2}' -f $userId, $signingInput, $signature))
}

[IO.File]::WriteAllText($outputPath, $builder.ToString(), [Text.UTF8Encoding]::new($false))
Write-Host "Generated $Count short-lived load-test users at $outputPath."
Write-Host "Tokens expire at $($expiresAt.ToLocalTime().ToString('yyyy-MM-dd HH:mm:ss zzz'))."
