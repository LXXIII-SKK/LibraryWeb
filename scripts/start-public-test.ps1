[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string]$PublicUrl,
    [string]$FrontendUrl,
    [string]$BackendUrl,
    [string]$KeycloakUrl,
    [string]$EnvFile = ".env.public-test",
    [switch]$SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-HttpsUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Value,
        [Parameter(Mandatory = $true)]
        [string]$Name
    )

    $uri = $null
    if (-not [System.Uri]::TryCreate($Value, [System.UriKind]::Absolute, [ref]$uri)) {
        throw "$Name must be an absolute URL."
    }

    if ($uri.Scheme -ne "https") {
        throw "$Name must use https."
    }

    return $uri.ToString().TrimEnd("/")
}

function Read-EnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path,
        [Parameter(Mandatory = $true)]
        [string]$Key
    )

    if (-not (Test-Path $Path)) { return $null }
    $line = Get-Content -Path $Path | Where-Object { $_ -match "^\s*$([regex]::Escape($Key))=" } | Select-Object -First 1
    if (-not $line) {
        return $null
    }

    return $line.Substring($Key.Length + 1).Trim()
}

function Wait-ForHttpReady {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Url,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 10
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 2
    }
    throw "Timed out waiting for $Url"
}

function Get-KeycloakAdminToken {
    param(
        [Parameter(Mandatory = $true)]
        [string]$LocalKeycloakBaseUrl
    )

    $username = Read-EnvValue -Path ".env" -Key "KEYCLOAK_BOOTSTRAP_ADMIN_USERNAME"
    if ([string]::IsNullOrWhiteSpace($username)) { $username = "admin" }

    $password = Read-EnvValue -Path ".env" -Key "KEYCLOAK_BOOTSTRAP_ADMIN_PASSWORD"
    if ([string]::IsNullOrWhiteSpace($password)) { $password = "admin" }

    try {
        $tokenResponse = Invoke-RestMethod `
            -Method Post `
            -Uri "$LocalKeycloakBaseUrl/realms/master/protocol/openid-connect/token" `
            -ContentType "application/x-www-form-urlencoded" `
            -Body @{
                client_id = "admin-cli"
                username = $username
                password = $password
                grant_type = "password"
            }
        return $tokenResponse.access_token
    } catch {
        throw "Failed to get Keycloak Admin token using username '$username'. Ensure .env matches the initialized Keycloak password. Error: $_"
    }
}

function Update-KeycloakClient {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PublicFrontendUrl,
        [Parameter(Mandatory = $true)]
        [string]$LocalKeycloakBaseUrl
    )

    $token = Get-KeycloakAdminToken -LocalKeycloakBaseUrl $LocalKeycloakBaseUrl
    $headers = @{ Authorization = "Bearer $token" }

    $realm = Read-EnvValue -Path ".env" -Key "KEYCLOAK_ADMIN_REALM"
    if ([string]::IsNullOrWhiteSpace($realm)) { $realm = "library" }

    $client = Invoke-RestMethod `
        -Method Get `
        -Uri "$LocalKeycloakBaseUrl/admin/realms/$realm/clients?clientId=library-web" `
        -Headers $headers |
        Select-Object -First 1

    if (-not $client) {
        throw "Keycloak client 'library-web' was not found."
    }

    $redirectUris = @(
        "http://localhost:3000/*",
        "http://localhost:5173/*",
        "$PublicFrontendUrl/*"
    ) | Select-Object -Unique

    $webOrigins = @(
        "http://localhost:3000",
        "http://localhost:5173",
        $PublicFrontendUrl
    ) | Select-Object -Unique

    $clientRepresentation = Invoke-RestMethod `
        -Method Get `
        -Uri "$LocalKeycloakBaseUrl/admin/realms/$realm/clients/$($client.id)" `
        -Headers $headers

    if (-not ($clientRepresentation.PSObject.Properties.Name -contains "redirectUris")) {
        $clientRepresentation | Add-Member -NotePropertyName redirectUris -NotePropertyValue @()
    }
    if (-not ($clientRepresentation.PSObject.Properties.Name -contains "webOrigins")) {
        $clientRepresentation | Add-Member -NotePropertyName webOrigins -NotePropertyValue @()
    }
    if (-not ($clientRepresentation.PSObject.Properties.Name -contains "rootUrl")) {
        $clientRepresentation | Add-Member -NotePropertyName rootUrl -NotePropertyValue $null
    }
    if (-not ($clientRepresentation.PSObject.Properties.Name -contains "baseUrl")) {
        $clientRepresentation | Add-Member -NotePropertyName baseUrl -NotePropertyValue $null
    }

    $clientRepresentation.redirectUris = $redirectUris
    $clientRepresentation.webOrigins = $webOrigins
    $clientRepresentation.rootUrl = $PublicFrontendUrl
    $clientRepresentation.baseUrl = $PublicFrontendUrl
    if (-not $clientRepresentation.attributes) {
        $clientRepresentation | Add-Member -NotePropertyName attributes -NotePropertyValue @{}
    }
    $clientRepresentation.attributes.'post.logout.redirect.uris' = "+"

    $body = $clientRepresentation | ConvertTo-Json -Depth 20

    Invoke-RestMethod `
        -Method Put `
        -Uri "$LocalKeycloakBaseUrl/admin/realms/$realm/clients/$($client.id)" `
        -Headers ($headers + @{ "Content-Type" = "application/json" }) `
        -Body $body | Out-Null
}

function Get-KeycloakLocalBaseUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string]$PublicKeycloakUrl
    )

    $keycloakUri = [System.Uri]$PublicKeycloakUrl
    $basePath = $keycloakUri.AbsolutePath.TrimEnd("/")
    if ([string]::IsNullOrEmpty($basePath)) {
        return "http://localhost:8081"
    }

    return "http://localhost:8081$basePath"
}

if (-not $PublicUrl -and (-not $FrontendUrl -or -not $BackendUrl -or -not $KeycloakUrl)) {
    if (-not (Test-Path $EnvFile)) {
        throw "Provide -PublicUrl or create $EnvFile from scripts/public-test.env.example."
    }
    $PublicUrl = Read-EnvValue -Path $EnvFile -Key "PUBLIC_TEST_URL"
}

$singleUrlMode = [string]::IsNullOrWhiteSpace($PublicUrl) -eq $false
if ($singleUrlMode) {
    $FrontendUrl = Assert-HttpsUrl -Value $PublicUrl -Name "PublicUrl"
    $BackendUrl = $FrontendUrl
    $KeycloakUrl = "$FrontendUrl/auth"
} else {
    $FrontendUrl = Assert-HttpsUrl -Value $FrontendUrl -Name "FrontendUrl"
    $BackendUrl = Assert-HttpsUrl -Value $BackendUrl -Name "BackendUrl"
    $KeycloakUrl = Assert-HttpsUrl -Value $KeycloakUrl -Name "KeycloakUrl"
}

$envBody = @(
    "PUBLIC_TEST_URL=$FrontendUrl"
)

if ($singleUrlMode) {
    $envBody += @(
        "KEYCLOAK_PUBLIC_URL=$KeycloakUrl"
        "KEYCLOAK_HTTP_RELATIVE_PATH=/auth"
        "KEYCLOAK_PROXY_PATH=/auth"
        "KEYCLOAK_ISSUER_URI=$KeycloakUrl/realms/library"
        "KEYCLOAK_JWK_SET_URI=http://keycloak:8080/auth/realms/library/protocol/openid-connect/certs"
        "APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,$FrontendUrl"
        "VITE_API_BASE_URL=$FrontendUrl"
        "VITE_KEYCLOAK_URL=$KeycloakUrl"
    )
} else {
    $envBody += @(
        "KEYCLOAK_PUBLIC_URL=$KeycloakUrl"
        "KEYCLOAK_HTTP_RELATIVE_PATH=/"
        "KEYCLOAK_PROXY_PATH="
        "KEYCLOAK_ISSUER_URI=$KeycloakUrl/realms/library"
        "KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/library/protocol/openid-connect/certs"
        "APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173,$FrontendUrl"
        "VITE_API_BASE_URL=$BackendUrl"
        "VITE_KEYCLOAK_URL=$KeycloakUrl"
    )
}

if ($PSCmdlet.ShouldProcess($EnvFile, "Write public test environment file")) {
    Set-Content -Path $EnvFile -Value $envBody
}

# The true fix: cascade the baseline .env first, then override with .env.public-test
$composeArgs = @("--env-file", ".env", "--env-file", $EnvFile, "up", "-d", "--force-recreate")
if (-not $SkipBuild) {
    $composeArgs += "--build"
}
$composeArgs += @("keycloak", "backend", "frontend")

Write-Host "Recreating Keycloak, backend, and frontend with public test URLs..."
if ($PSCmdlet.ShouldProcess("docker compose", "Recreate keycloak, backend, and frontend")) {
    docker compose @composeArgs
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed."
    }
}

if ($PSCmdlet.ShouldProcess("Keycloak client library-web", "Apply public redirect and origin settings")) {
    $localKeycloakBaseUrl = Get-KeycloakLocalBaseUrl -PublicKeycloakUrl $KeycloakUrl
    Wait-ForHttpReady -Url "$localKeycloakBaseUrl/realms/library/.well-known/openid-configuration"
    Update-KeycloakClient -PublicFrontendUrl $FrontendUrl -LocalKeycloakBaseUrl $localKeycloakBaseUrl
}

Write-Host ""
if ($WhatIfPreference) {
    Write-Host "Dry run complete."
} else {
    Write-Host "Public test environment is ready."
}
Write-Host "Frontend:  $FrontendUrl"
Write-Host "Backend:   $BackendUrl"
Write-Host "Keycloak:  $KeycloakUrl"
Write-Host ""
if ($WhatIfPreference) {
    Write-Host "Run again without -WhatIf to apply the public test setup."
} else {
    Write-Host "Share the frontend URL with testers."
}
