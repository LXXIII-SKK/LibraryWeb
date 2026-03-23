[CmdletBinding(SupportsShouldProcess = $true, ConfirmImpact = 'High')]
param(
    [ValidateSet('auto', 'docker', 'local')]
    [string]$Mode = 'auto',

    [string]$ComposeService = 'postgres',

    [string]$DbUrl = $env:DB_URL,

    [string]$DbHost = $env:DB_HOST,

    [int]$DbPort = 0,

    [string]$DbName = $env:DB_NAME,

    [string]$DbUser = $(if ($env:DB_USERNAME) { $env:DB_USERNAME } else { 'library_app' }),

    [string]$DbPassword = $(if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'library_app_pw' })
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$sqlFile = Join-Path $PSScriptRoot 'reset-book-viewcounts.sql'

if (-not (Test-Path -LiteralPath $sqlFile)) {
    throw "SQL file not found: $sqlFile"
}

if ($DbUrl -match '^jdbc:postgresql://(?<host>[^/:?]+)(:(?<port>\d+))?/(?<database>[^?]+)') {
    if (-not $DbHost) {
        $DbHost = $Matches.host
    }
    if ($DbPort -eq 0 -and $Matches.port) {
        $DbPort = [int]$Matches.port
    }
    if (-not $DbName) {
        $DbName = $Matches.database
    }
}

if (-not $DbHost) {
    $DbHost = 'localhost'
}

if ($DbPort -eq 0) {
    $DbPort = 5432
}

if (-not $DbName) {
    $DbName = 'library'
}

$sql = Get-Content -Path $sqlFile -Raw

function Test-CommandAvailable {
    param([string]$Name)

    return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Test-DockerComposeServiceRunning {
    if (-not (Test-CommandAvailable 'docker')) {
        return $false
    }

    $serviceNames = & docker compose ps --services --status running $ComposeService 2>$null
    if ($LASTEXITCODE -ne 0) {
        return $false
    }

    return ($serviceNames | ForEach-Object { $_.Trim() }) -contains $ComposeService
}

function Invoke-DockerReset {
    Write-Host "Using Docker Compose service '$ComposeService' for database '$DbName'."

    if ($PSCmdlet.ShouldProcess("docker compose service '$ComposeService'", "delete VIEWED rows from activity_log")) {
        $sql | & docker compose exec -T $ComposeService psql -v ON_ERROR_STOP=1 -U $DbUser -d $DbName
        if ($LASTEXITCODE -ne 0) {
            throw "Reset failed while executing psql in Docker Compose service '$ComposeService'."
        }
    }
}

function Invoke-LocalReset {
    if (-not (Test-CommandAvailable 'psql')) {
        throw "psql was not found. Install PostgreSQL client tools or run the script with -Mode docker."
    }

    Write-Host "Using local psql against ${DbHost}:$DbPort/$DbName."

    if ($PSCmdlet.ShouldProcess("${DbHost}:$DbPort/$DbName", "delete VIEWED rows from activity_log")) {
        $previousPgPassword = $env:PGPASSWORD

        try {
            $env:PGPASSWORD = $DbPassword
            $sql | & psql -v ON_ERROR_STOP=1 -h $DbHost -p $DbPort -U $DbUser -d $DbName
            if ($LASTEXITCODE -ne 0) {
                throw "Reset failed while executing local psql."
            }
        }
        finally {
            if ($null -ne $previousPgPassword) {
                $env:PGPASSWORD = $previousPgPassword
            }
            else {
                Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
            }
        }
    }
}

$selectedMode = switch ($Mode) {
    'auto' {
        if (Test-DockerComposeServiceRunning) {
            'docker'
        }
        elseif (Test-CommandAvailable 'psql') {
            'local'
        }
        else {
            throw "No database client detected. Start the Docker Compose postgres service or install psql."
        }
    }
    default {
        $Mode
    }
}

Write-Host "Resetting book view counts by deleting VIEWED rows from activity_log."

switch ($selectedMode) {
    'docker' {
        if (-not (Test-DockerComposeServiceRunning)) {
            throw "Docker Compose service '$ComposeService' is not running."
        }
        Invoke-DockerReset
    }
    'local' {
        Invoke-LocalReset
    }
    default {
        throw "Unsupported mode: $selectedMode"
    }
}

Write-Host 'Book view count reset completed.'
