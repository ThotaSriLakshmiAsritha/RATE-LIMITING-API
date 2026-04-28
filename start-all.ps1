#!/usr/bin/env pwsh
# Start all services: Backend, Frontend, and Docker stack (Redis, Postgres, etc.)

Write-Host "[>>] Starting Rate-Limiting Application Stack..." -ForegroundColor Cyan

# Kill any existing processes on ports
$portsToCheck = @(8080, 5173)
foreach ($port in $portsToCheck) {
    $process = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "[!!] Freeing port $port..." -ForegroundColor Yellow
        Stop-Process -Id $process.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}

# Start Docker Compose (Redis, Postgres, Prometheus, Grafana in background)
Write-Host "`n[*] Starting Docker services (Redis, Postgres)..." -ForegroundColor Green
Start-Process -NoNewWindow -FilePath "docker" -ArgumentList "compose", "up", "-d" -WorkingDirectory $PSScriptRoot

# Wait a moment for Docker services to be healthy
Write-Host "[*] Waiting for Docker services to be healthy..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# Start Backend (Spring Boot) in new window
Write-Host "`n[*] Starting Backend (Spring Boot on port 8080)..." -ForegroundColor Green
Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot'; mvn.cmd spring-boot:run" -WindowStyle Normal

# Wait for backend to be ready
Write-Host "[*] Waiting for backend to be ready..." -ForegroundColor Yellow
$backendReady = $false
$retries = 0
while (-not $backendReady -and $retries -lt 60) {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/ping" -UseBasicParsing -TimeoutSec 2 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            $backendReady = $true
            Write-Host "[OK] Backend is ready!" -ForegroundColor Green
        }
    } catch {
        $retries++
        Start-Sleep -Seconds 1
    }
}

if (-not $backendReady) {
    Write-Host "[!!] Backend did not start in time. Continuing anyway..." -ForegroundColor Yellow
}

# Start Frontend (React + Vite on port 5173) in new window
Write-Host "`n[*] Starting Frontend (React on port 5173)..." -ForegroundColor Green
Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "cd '$PSScriptRoot/frontend-dashboard'; npm run dev" -WindowStyle Normal

Write-Host "`n[OK] All services starting..." -ForegroundColor Cyan
Write-Host "   Backend:  http://localhost:8080/ping" -ForegroundColor Yellow
Write-Host "   Frontend: http://localhost:5173" -ForegroundColor Yellow
Write-Host "   Redis:    localhost:6379" -ForegroundColor Yellow
Write-Host "   Postgres: localhost:5432" -ForegroundColor Yellow
Write-Host "`n[TIP] Run 'docker compose down' to stop all Docker services" -ForegroundColor Blue
