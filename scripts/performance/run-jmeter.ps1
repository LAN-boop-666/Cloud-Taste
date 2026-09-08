[CmdletBinding()]
param(
    [ValidateSet('order', 'query')]
    [string]$Scenario = 'order',
    [ValidateRange(1, 1000)]
    [int]$Threads = 5,
    [ValidateRange(0, 3600)]
    [int]$RampUp = 5,
    [ValidateRange(1, 10000)]
    [int]$Loops = 1,
    [switch]$Overwrite
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$jmeter = Join-Path $projectRoot '.tools\apache-jmeter\bin\jmeter.bat'
$users = Join-Path $projectRoot '.tools\loadtest-users.csv'
$resultsRoot = Join-Path $projectRoot '.tools\loadtest-results'
$plan = if ($Scenario -eq 'order') { 'order-load-test.jmx' } else { 'order-query-load-test.jmx' }
$planPath = Join-Path $PSScriptRoot $plan
$runName = '{0}-t{1}-r{2}-l{3}' -f $Scenario, $Threads, $RampUp, $Loops
$jtl = Join-Path $resultsRoot ($runName + '.jtl')
$report = Join-Path $resultsRoot ($runName + '-report')

foreach ($required in @($jmeter, $users, $planPath)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required file not found: $required"
    }
}
if (-not (Get-NetTCPConnection -State Listen -LocalPort 8081 -ErrorAction SilentlyContinue)) {
    throw 'The isolated backend is not listening on 127.0.0.1:8081.'
}

New-Item -ItemType Directory -Force -Path $resultsRoot | Out-Null
if ((Test-Path $jtl) -or (Test-Path $report)) {
    if (-not $Overwrite) {
        throw "Results already exist for $runName. Use -Overwrite to replace them."
    }
    if (Test-Path $jtl) { [IO.File]::Delete($jtl) }
    if (Test-Path $report) { [IO.Directory]::Delete($report, $true) }
}

& $jmeter -n -t $planPath "-Jthreads=$Threads" "-JrampUp=$RampUp" "-Jloops=$Loops" "-JusersFile=$users" -l $jtl -e -o $report
if ($LASTEXITCODE -ne 0) {
    throw "JMeter failed with exit code $LASTEXITCODE."
}

$rows = @(Import-Csv $jtl)
$elapsed = @($rows | ForEach-Object { [int]$_.elapsed } | Sort-Object)
$errors = @($rows | Where-Object { $_.success -ne 'true' }).Count
$p95Index = [Math]::Max(0, [int][Math]::Ceiling($elapsed.Count * 0.95) - 1)
$p99Index = [Math]::Max(0, [int][Math]::Ceiling($elapsed.Count * 0.99) - 1)
[pscustomobject]@{
    Run       = $runName
    Samples   = $rows.Count
    Errors    = $errors
    ErrorRate = '{0:P2}' -f ($errors / $rows.Count)
    AverageMs = [Math]::Round((($elapsed | Measure-Object -Average).Average), 2)
    P95Ms     = $elapsed[$p95Index]
    P99Ms     = $elapsed[$p99Index]
    MaximumMs = $elapsed[-1]
    Report    = $report
} | Format-List
