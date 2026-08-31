[CmdletBinding()]
param(
    [string]$NdkHome = "$env:LOCALAPPDATA\Android\Sdk\ndk\28.2.13676358",
    [string]$OutputAar = (Join-Path $PSScriptRoot "..\android\app\libs\tokenizer-native-0.33.0-16k.aar")
)

$ErrorActionPreference = "Stop"
$sourceTag = "v0.33.0"
$sourceCommit = "39f5fa8b2e4e362613379caf8e6715a08ea93cac"
$loadAlignment = 16KB
$rustFlags = "-C link-arg=-Wl,-z,max-page-size=16384 -C link-arg=-Wl,-z,common-page-size=16384"

function Assert-LastExitCode([string]$Step) {
    if ($LASTEXITCODE -ne 0) {
        throw "$Step failed with exit code $LASTEXITCODE"
    }
}

function Import-VisualStudioEnvironment {
    if (Get-Command link.exe -ErrorAction SilentlyContinue) {
        return
    }

    $vswhere = "C:\Program Files (x86)\Microsoft Visual Studio\Installer\vswhere.exe"
    if (-not (Test-Path $vswhere)) {
        throw "Visual Studio Build Tools with the C++ workload is required."
    }
    $installation = & $vswhere -latest -products "*" -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath
    if (-not $installation) {
        throw "Visual Studio C++ build tools were not found."
    }

    $devCmd = Join-Path $installation "Common7\Tools\VsDevCmd.bat"
    $environment = & cmd.exe /d /s /c "`"$devCmd`" -arch=x64 >nul && set"
    Assert-LastExitCode "Visual Studio environment setup"
    foreach ($line in $environment) {
        if ($line -match "^([^=]+)=(.*)$") {
            Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
        }
    }
}

function Get-ElfLoadAlignments([byte[]]$Bytes) {
    if ($Bytes.Length -lt 64 -or $Bytes[0] -ne 0x7f -or $Bytes[1] -ne 0x45) {
        throw "Input is not an ELF binary."
    }
    if ($Bytes[5] -ne 1) {
        throw "Only little-endian Android ELF binaries are supported."
    }

    $is64Bit = $Bytes[4] -eq 2
    if ($is64Bit) {
        $programHeaderOffset = [int][BitConverter]::ToUInt64($Bytes, 32)
        $entrySize = [BitConverter]::ToUInt16($Bytes, 54)
        $entryCount = [BitConverter]::ToUInt16($Bytes, 56)
        $alignmentOffset = 48
    } else {
        $programHeaderOffset = [int][BitConverter]::ToUInt32($Bytes, 28)
        $entrySize = [BitConverter]::ToUInt16($Bytes, 42)
        $entryCount = [BitConverter]::ToUInt16($Bytes, 44)
        $alignmentOffset = 28
    }

    $alignments = @()
    for ($index = 0; $index -lt $entryCount; $index++) {
        $offset = $programHeaderOffset + ($index * $entrySize)
        if ([BitConverter]::ToUInt32($Bytes, $offset) -ne 1) {
            continue
        }
        $alignment = if ($is64Bit) {
            [BitConverter]::ToUInt64($Bytes, $offset + $alignmentOffset)
        } else {
            [BitConverter]::ToUInt32($Bytes, $offset + $alignmentOffset)
        }
        $alignments += [uint64]$alignment
    }
    return $alignments
}

if (-not (Test-Path (Join-Path $NdkHome "source.properties"))) {
    throw "Android NDK r28c was not found at $NdkHome"
}

Import-VisualStudioEnvironment
$cargoBin = Join-Path $env:USERPROFILE ".cargo\bin"
if (($env:PATH -split ";") -notcontains $cargoBin) {
    $env:PATH = "$cargoBin;$env:PATH"
}
foreach ($command in @("git.exe", "cargo.exe", "rustup.exe")) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "$command is required."
    }
}
if (-not (Test-Path (Join-Path (Split-Path (Get-Command cargo.exe).Source) "cargo-ndk.exe"))) {
    throw "cargo-ndk is required. Install the pinned 4.1.x release first."
}

$workRoot = Join-Path $env:TEMP ("djl-tokenizer-16k-" + [guid]::NewGuid().ToString("N"))
$sourceRoot = Join-Path $workRoot "djl"
New-Item -ItemType Directory -Path $workRoot | Out-Null

& git clone --depth 1 --branch $sourceTag https://github.com/deepjavalibrary/djl.git $sourceRoot
Assert-LastExitCode "DJL clone"
$resolvedCommit = (& git -C $sourceRoot rev-parse HEAD).Trim()
Assert-LastExitCode "DJL source verification"
if ($resolvedCommit -ne $sourceCommit) {
    throw "DJL $sourceTag resolved to $resolvedCommit instead of $sourceCommit"
}

$targets = @(
    "aarch64-linux-android",
    "armv7-linux-androideabi",
    "x86_64-linux-android",
    "i686-linux-android"
)
& rustup target add @targets
Assert-LastExitCode "Rust Android target installation"

$env:ANDROID_NDK_HOME = $NdkHome
$env:ANDROID_NDK = $NdkHome
$env:CARGO_NET_RETRY = "10"
$env:CARGO_HTTP_MULTIPLEXING = "false"
$env:CARGO_TARGET_AARCH64_LINUX_ANDROID_RUSTFLAGS = $rustFlags
$env:CARGO_TARGET_ARMV7_LINUX_ANDROIDEABI_RUSTFLAGS = $rustFlags
$env:CARGO_TARGET_X86_64_LINUX_ANDROID_RUSTFLAGS = $rustFlags
$env:CARGO_TARGET_I686_LINUX_ANDROID_RUSTFLAGS = $rustFlags

Push-Location (Join-Path $sourceRoot "extensions\tokenizers\rust")
try {
    # cargo-ndk 4.1.2 can panic while copying multi-ABI outputs on Windows.
    # Let Cargo keep each result in its target directory and package it below.
    & cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 -t x86 --platform 21 build --release
    Assert-LastExitCode "DJL Android tokenizer build"
} finally {
    Pop-Location
}

$aarJni = Join-Path $sourceRoot "android\tokenizer-native\jnilib"
$targetByAbi = [ordered]@{
    "arm64-v8a" = "aarch64-linux-android"
    "armeabi-v7a" = "armv7-linux-androideabi"
    "x86_64" = "x86_64-linux-android"
    "x86" = "i686-linux-android"
}
foreach ($abi in $targetByAbi.Keys) {
    $sourceLibrary = Join-Path $sourceRoot "extensions\tokenizers\rust\target\$($targetByAbi[$abi])\release\libdjl_tokenizer.so"
    if (-not (Test-Path $sourceLibrary)) {
        throw "Missing native tokenizer for $abi"
    }
    $destinationDirectory = Join-Path $aarJni $abi
    New-Item -ItemType Directory -Force -Path $destinationDirectory | Out-Null
    Copy-Item -LiteralPath $sourceLibrary -Destination $destinationDirectory -Force
}

$env:ANDROID_HOME = Split-Path (Split-Path $NdkHome -Parent) -Parent
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
& (Join-Path $sourceRoot "gradlew.bat") -p (Join-Path $sourceRoot "android") :tokenizer-native:assembleRelease
Assert-LastExitCode "Tokenizer AAR assembly"
$builtAar = Join-Path $sourceRoot "android\tokenizer-native\build\outputs\aar\tokenizer-native-release.aar"
if (-not (Test-Path $builtAar)) {
    throw "The tokenizer AAR was not produced."
}

Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [IO.Compression.ZipFile]::OpenRead($builtAar)
try {
    $libraries = $archive.Entries | Where-Object {
        $_.FullName -match "^jni/[^/]+/libdjl_tokenizer[.]so$"
    }
    if ($libraries.Count -ne 4) {
        throw "Expected four tokenizer native libraries, found $($libraries.Count)."
    }
    foreach ($entry in $libraries) {
        $memory = New-Object IO.MemoryStream
        $stream = $entry.Open()
        try {
            $stream.CopyTo($memory)
            $alignments = Get-ElfLoadAlignments $memory.ToArray()
        } finally {
            $stream.Dispose()
            $memory.Dispose()
        }
        if (-not $alignments -or ($alignments | Where-Object { $_ -lt $loadAlignment })) {
            throw "$($entry.FullName) is not 16 KB aligned: $($alignments -join ', ')"
        }
        Write-Host "$($entry.FullName): $($alignments -join ', ')"
    }
} finally {
    $archive.Dispose()
}

$resolvedOutput = [IO.Path]::GetFullPath($OutputAar)
New-Item -ItemType Directory -Force -Path (Split-Path $resolvedOutput) | Out-Null
Copy-Item -LiteralPath $builtAar -Destination $resolvedOutput -Force
$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedOutput).Hash.ToLowerInvariant()
Write-Host "AAR: $resolvedOutput"
Write-Host "SHA-256: $hash"
Write-Host "Build workspace retained at: $workRoot"
