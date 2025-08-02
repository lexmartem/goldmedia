# Docker Compose Management Script for Video Metadata Service
# Usage: .\start-docker.ps1 [command]

param(
    [Parameter(Position=0)]
    [ValidateSet("start", "stop", "restart", "logs", "status", "clean", "build")]
    [string]$Command = "start"
)

$ComposeFile = "docker-compose.yml"
$ServiceName = "video-metadata-service"

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Warning {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

function Test-Docker {
    try {
        docker --version | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

function Test-DockerCompose {
    try {
        docker-compose --version | Out-Null
        return $true
    }
    catch {
        return $false
    }
}

function Start-Services {
    Write-Info "Starting Video Metadata Service with Redis..."
    
    if (-not (Test-Path $ComposeFile)) {
        Write-Error "docker-compose.yml not found in current directory"
        exit 1
    }
    
    Write-Info "Building and starting services..."
    docker-compose up --build -d
    
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Services started successfully!"
        Write-Info "Application will be available at: http://localhost:8080"
        Write-Info "Swagger UI: http://localhost:8080/swagger-ui.html"
        Write-Info "Redis is available at: localhost:6379"
        Write-Info ""
        Write-Info "Default credentials:"
        Write-Info "  Admin: admin / admin123"
        Write-Info "  User:  user  / user123"
        Write-Info ""
        Write-Info "To view logs: .\start-docker.ps1 logs"
        Write-Info "To stop services: .\start-docker.ps1 stop"
    }
    else {
        Write-Error "Failed to start services"
        exit 1
    }
}

function Stop-Services {
    Write-Info "Stopping services..."
    docker-compose down
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Services stopped successfully!"
    }
    else {
        Write-Error "Failed to stop services"
    }
}

function Restart-Services {
    Write-Info "Restarting services..."
    Stop-Services
    Start-Sleep -Seconds 2
    Start-Services
}

function Show-Logs {
    Write-Info "Showing logs (Press Ctrl+C to exit)..."
    docker-compose logs -f
}

function Show-Status {
    Write-Info "Service Status:"
    docker-compose ps
    
    Write-Info ""
    Write-Info "Container Health:"
    docker-compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}"
}

function Clean-All {
    Write-Warning "This will remove all containers, images, and volumes!"
    $confirmation = Read-Host "Are you sure? (y/N)"
    if ($confirmation -eq "y" -or $confirmation -eq "Y") {
        Write-Info "Cleaning up Docker resources..."
        docker-compose down -v --rmi all
        docker system prune -f
        Write-Info "Cleanup completed!"
    }
    else {
        Write-Info "Cleanup cancelled."
    }
}

function Build-Only {
    Write-Info "Building application image..."
    docker-compose build
    if ($LASTEXITCODE -eq 0) {
        Write-Info "Build completed successfully!"
    }
    else {
        Write-Error "Build failed"
    }
}

# Main execution
if (-not (Test-Docker)) {
    Write-Error "Docker is not installed or not in PATH"
    exit 1
}

if (-not (Test-DockerCompose)) {
    Write-Error "Docker Compose is not installed or not in PATH"
    exit 1
}

Write-Info "Video Metadata Service Docker Management"
Write-Info "========================================"

switch ($Command) {
    "start" { Start-Services }
    "stop" { Stop-Services }
    "restart" { Restart-Services }
    "logs" { Show-Logs }
    "status" { Show-Status }
    "clean" { Clean-All }
    "build" { Build-Only }
    default {
        Write-Info "Available commands:"
        Write-Info "  start   - Start all services"
        Write-Info "  stop    - Stop all services"
        Write-Info "  restart - Restart all services"
        Write-Info "  logs    - Show service logs"
        Write-Info "  status  - Show service status"
        Write-Info "  clean   - Remove all containers and images"
        Write-Info "  build   - Build application image only"
    }
} 