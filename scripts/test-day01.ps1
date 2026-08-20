param(
    [string]$BaseUrl = 'http://127.0.0.1:8080'
)

$ErrorActionPreference = 'Stop'

try {
    $doc = Invoke-WebRequest -Uri "$BaseUrl/doc.html" -UseBasicParsing -TimeoutSec 10
    if ($doc.StatusCode -ne 200) {
        throw "Unexpected API documentation status: $($doc.StatusCode)"
    }

    $badLogin = @{ username = '__cloud_taste_missing_user__'; password = 'invalid' } | ConvertTo-Json
    $response = Invoke-RestMethod -Uri "$BaseUrl/admin/employee/login" -Method Post `
        -ContentType 'application/json' -Body $badLogin -TimeoutSec 10
    if ($response.code -eq 1) {
        throw 'A missing user must not be able to log in.'
    }

    Write-Host 'Day01 smoke test passed: API docs are reachable and invalid login is rejected.'
}
catch {
    throw "Day01 smoke test failed: $($_.Exception.Message)"
}
