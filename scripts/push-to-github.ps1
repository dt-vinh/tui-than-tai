param(
    [Parameter(Mandatory = $true)]
    [string]$RepoUrl,

    [string]$Branch = "main",

    [string]$CommitMessage = "Initial Lucky Wallet app"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Git = Get-Command git -ErrorAction SilentlyContinue

if (-not $Git) {
    throw "git is not installed or is not on PATH. Install Git for Windows, then rerun this script."
}

Push-Location $Root
try {
    if (-not (Test-Path ".git")) {
        & $Git.Source init
    }

    $currentBranch = (& $Git.Source branch --show-current 2>$null) -join ""
    if (-not $currentBranch) {
        & $Git.Source checkout -B $Branch
    } elseif ($currentBranch -ne $Branch) {
        Write-Host "Current branch is '$currentBranch'. Using it instead of switching to '$Branch'."
        $Branch = $currentBranch
    }

    $paths = @(
        "README.md",
        ".gitignore",
        "android",
        "backend",
        "docs",
        "design",
        "mockups",
        "scripts",
        "research/frames"
    )

    & $Git.Source add -- $paths

    $status = (& $Git.Source status --porcelain) -join ""
    if ($status) {
        & $Git.Source commit -m $CommitMessage
    } else {
        Write-Host "No source changes to commit."
    }

    $remote = (& $Git.Source remote get-url origin 2>$null) -join ""
    if ($remote) {
        & $Git.Source remote set-url origin $RepoUrl
    } else {
        & $Git.Source remote add origin $RepoUrl
    }

    & $Git.Source push -u origin $Branch
} finally {
    Pop-Location
}
