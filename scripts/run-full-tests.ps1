param(
    [switch]$SkipEmulator
)

$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Android = Join-Path $Root "android"
$Backend = Join-Path $Root "backend"
$Results = Join-Path $Root "test-results"
$AvdName = "SafeSign_API35"
$PackageName = "com.phuongnn14.tuithantai"

New-Item -ItemType Directory -Force -Path $Results | Out-Null

function Resolve-AndroidTool($RelativePath, $CommandName) {
    $candidates = @()
    if ($env:ANDROID_HOME) {
        $candidates += (Join-Path $env:ANDROID_HOME $RelativePath)
    }
    if ($env:ANDROID_SDK_ROOT) {
        $candidates += (Join-Path $env:ANDROID_SDK_ROOT $RelativePath)
    }
    $candidates += (Join-Path "D:\Android\Sdk" $RelativePath)

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    $cmd = Get-Command $CommandName -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    throw "Cannot find Android tool: $CommandName"
}

$Adb = Resolve-AndroidTool "platform-tools\adb.exe" "adb"
$Emulator = Resolve-AndroidTool "emulator\emulator.exe" "emulator"

function Step($Name, [scriptblock]$Block) {
    Write-Host "`n=== $Name ==="
    & $Block
    Write-Host "PASS: $Name"
}

function In-Directory($Path, [scriptblock]$Block) {
    Push-Location $Path
    try {
        & $Block
    } finally {
        Pop-Location
    }
}

function Ensure-Emulator {
    & $Adb start-server | Out-Null
    $devices = & $Adb devices | Where-Object { $_ -match "\tdevice$" }
    if (-not $devices) {
        Write-Host "Starting AVD $AvdName..."
        Start-Process -FilePath $Emulator -ArgumentList @("-avd", $AvdName, "-no-snapshot-load", "-no-snapshot-save", "-no-boot-anim") -WorkingDirectory (Split-Path $Emulator) -WindowStyle Hidden
        & $Adb wait-for-device
    }

    $deadline = (Get-Date).AddMinutes(8)
    do {
        Start-Sleep -Seconds 5
        $boot = (& $Adb shell getprop sys.boot_completed 2>$null) -join ""
        Write-Host "boot=$boot"
        if ($boot -match "1") {
            return
        }
    } while ((Get-Date) -lt $deadline)

    throw "Emulator did not finish booting"
}

function Dump-Ui($Name) {
    $remote = "/sdcard/$Name.xml"
    $local = Join-Path $Results "$Name.xml"
    & $Adb shell uiautomator dump $remote | Out-Host
    & $Adb pull $remote $local | Out-Host
    return Get-Content $local -Raw -Encoding UTF8
}

function Screenshot($Name) {
    $remote = "/sdcard/$Name.png"
    $local = Join-Path $Results "$Name.png"
    & $Adb shell screencap -p $remote
    & $Adb pull $remote $local | Out-Host
}

function Assert-Contains($Text, $Needle, $Context) {
    if ($Text -notlike "*$Needle*") {
        throw "Expected '$Needle' in $Context"
    }
}

Step "Backend API tests" {
    In-Directory $Backend {
        npm test
    }
}

Step "Android JVM unit tests" {
    In-Directory $Android {
        .\gradlew.bat :app:testDebugUnitTest --no-daemon
    }
}

Step "Android debug APK build" {
    In-Directory $Android {
        .\gradlew.bat :app:assembleDebug --no-daemon
    }
}

Step "Android release AAB build" {
    In-Directory $Android {
        .\gradlew.bat :app:bundleRelease --no-daemon "-Dkotlin.compiler.execution.strategy=in-process"
    }
}

if (-not $SkipEmulator) {
    Step "Emulator install and smoke flow" {
        Ensure-Emulator
        & $Adb shell pm disable-user --user 0 com.google.android.apps.wellbeing | Out-Host
        & $Adb install -r (Join-Path $Android "app\build\outputs\apk\debug\app-debug.apk") | Out-Host
        & $Adb shell pm clear $PackageName | Out-Host
        & $Adb shell am start -n "$PackageName/.MainActivity" | Out-Host
        Start-Sleep -Seconds 12

        $ui = Dump-Ui "onboarding"
        Screenshot "onboarding"
        Assert-Contains $ui "Scan" "onboarding"
        Assert-Contains $ui "ML Kit" "onboarding"

        & $Adb shell input tap 540 1370
        Start-Sleep -Seconds 4
        $homeUi = Dump-Ui "home"
        Screenshot "home"
        Assert-Contains $homeUi "Lucky Wallet" "home"
        Assert-Contains $homeUi "Manual entry" "home"
        Assert-Contains $homeUi "Reports" "home"
        Assert-Contains $homeUi "No expenses yet" "home"

        & $Adb shell input tap 760 2295
        Start-Sleep -Seconds 3
        $tools = Dump-Ui "tools"
        Screenshot "tools"
        Assert-Contains $tools "Momo" "tools"
        Assert-Contains $tools "Internet" "tools"
        Assert-Contains $tools "backend PC" "tools"
    }
}

Set-Content -Path (Join-Path $Results "summary.txt") -Value "All selected checks passed at $(Get-Date -Format o)" -Encoding UTF8
Write-Host "`nAll selected checks passed."
