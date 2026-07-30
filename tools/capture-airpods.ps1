[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        "anc",
        "transparency",
        "adaptive",
        "left-ear",
        "right-ear",
        "case-open",
        "charging"
    )]
    [string]$Scenario,

    [string]$Serial,

    [ValidateRange(3, 10)]
    [int]$Repetitions = 3,

    [ValidateRange(3, 30)]
    [int]$SettleSeconds = 8
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$adbFromSdk = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$adb = if (Test-Path -LiteralPath $adbFromSdk) {
    $adbFromSdk
} else {
    (Get-Command adb -ErrorAction Stop).Source
}

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)

    $output = & $adb -s $Serial @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "ADB falló: adb -s $Serial $($Arguments -join ' ')"
    }
    return $output
}

$connected = & $adb devices
$available = @(
    $connected |
        Select-String -Pattern "^([^\s]+)\s+device$" |
        ForEach-Object { $_.Matches[0].Groups[1].Value }
)

if ([string]::IsNullOrWhiteSpace($Serial)) {
    $physical = @($available | Where-Object { $_ -notmatch "^emulator-" })
    if ($physical.Count -ne 1) {
        throw "Conecta exactamente un teléfono físico o indica -Serial. Detectados: $($available -join ', ')"
    }
    $Serial = $physical[0]
}

if ($Serial -match "^emulator-") {
    throw "La captura propietaria requiere un teléfono físico; el emulador no transporta señales reales de AirPods."
}
if ($Serial -notin $available) {
    throw "El dispositivo '$Serial' no aparece como autorizado en 'adb devices'."
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$captureRoot = Join-Path $projectRoot "captures\airpods\$timestamp-$Scenario"
New-Item -ItemType Directory -Path $captureRoot -Force | Out-Null

$deviceSummary = Invoke-Adb shell getprop ro.product.manufacturer
$deviceModel = Invoke-Adb shell getprop ro.product.model
$androidVersion = Invoke-Adb shell getprop ro.build.version.release
$sdkLevel = Invoke-Adb shell getprop ro.build.version.sdk

$metadata = [ordered]@{
    scenario = $Scenario
    repetitions = $Repetitions
    settleSeconds = $SettleSeconds
    startedAt = (Get-Date).ToString("o")
    manufacturer = ($deviceSummary -join "").Trim()
    model = ($deviceModel -join "").Trim()
    android = ($androidVersion -join "").Trim()
    sdk = ($sdkLevel -join "").Trim()
    serialStored = $false
    transitions = @()
}

Write-Host ""
Write-Host "Preparación obligatoria en el teléfono:" -ForegroundColor Cyan
Write-Host "1. Activa Opciones de desarrollador."
Write-Host "2. Activa 'Registro de rastreo Bluetooth HCI'."
Write-Host "3. Apaga y vuelve a encender Bluetooth desde el teléfono."
Write-Host "4. Conecta los AirPods y comprueba que reproducen audio."
Write-Host ""
Write-Warning "El bugreport contiene información sensible. Permanecerá en captures/, carpeta ignorada por Git."
Read-Host "Pulsa Enter cuando todo esté listo"

Invoke-Adb logcat -c | Out-Null
Invoke-Adb shell dumpsys bluetooth_manager |
    Set-Content -LiteralPath (Join-Path $captureRoot "bluetooth-before.txt") -Encoding utf8

for ($index = 1; $index -le $Repetitions; $index++) {
    Write-Host ""
    Write-Host "Repetición $index de $Repetitions" -ForegroundColor Yellow
    Read-Host "Deja los AirPods en el estado de CONTROL y pulsa Enter"

    $controlAt = (Get-Date).ToString("o")
    Invoke-Adb shell log -t AirPodsCapture "scenario=$Scenario repetition=$index phase=control" | Out-Null

    Read-Host "Realiza AHORA la transición a '$Scenario' y pulsa Enter inmediatamente"
    $actionAt = (Get-Date).ToString("o")
    Invoke-Adb shell log -t AirPodsCapture "scenario=$Scenario repetition=$index phase=action" | Out-Null
    Start-Sleep -Seconds $SettleSeconds
    Invoke-Adb shell log -t AirPodsCapture "scenario=$Scenario repetition=$index phase=end" | Out-Null

    $metadata.transitions += [ordered]@{
        repetition = $index
        controlAt = $controlAt
        actionAt = $actionAt
        completedAt = (Get-Date).ToString("o")
    }
}

Invoke-Adb shell dumpsys bluetooth_manager |
    Set-Content -LiteralPath (Join-Path $captureRoot "bluetooth-after.txt") -Encoding utf8
Invoke-Adb logcat -d -b all |
    Set-Content -LiteralPath (Join-Path $captureRoot "logcat.txt") -Encoding utf8

$metadata.completedAt = (Get-Date).ToString("o")
$metadata | ConvertTo-Json -Depth 5 |
    Set-Content -LiteralPath (Join-Path $captureRoot "capture-metadata.json") -Encoding utf8

Write-Host ""
Write-Host "Generando bugreport; Android puede tardar varios minutos..." -ForegroundColor Cyan
& $adb -s $Serial bugreport $captureRoot
if ($LASTEXITCODE -ne 0) {
    Write-Warning "No se pudo generar el bugreport. Logcat y dumpsys sí quedaron guardados."
}

Write-Host ""
Write-Host "Captura terminada:" -ForegroundColor Green
Write-Host $captureRoot
Write-Host "No publiques ni subas el ZIP sin revisarlo; puede contener identificadores y datos del teléfono."
