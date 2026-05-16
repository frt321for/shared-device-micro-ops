param(
    [Parameter(Position=0)]
    [ValidateSet("status", "start", "stop", "restart", "logs", "tunnel", "backend", "frontend")]
    [string]$Command = "status",

    [Parameter(Position=1)]
    [string]$Target = ""
)

$PROJECT_DIR = Split-Path -Parent $PSScriptRoot
$BACKEND_DIR = Join-Path $PROJECT_DIR "src\backend"
$FRONTEND_DIR = Join-Path $PROJECT_DIR "src\frontend"

$PID_FILE = Join-Path $PROJECT_DIR "scripts\.pids"

function Read-Pids {
    if (Test-Path $PID_FILE) {
        $raw = Get-Content $PID_FILE -Raw
        # Validate format before converting
        if ($raw -match '^\w+=\d+') {
            return $raw | ConvertFrom-StringData
        }
    }
    return @{}
}

function Write-Pids($pids) {
    $lines = $pids.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }
    $lines -join "`n" | Out-File -FilePath $PID_FILE -Encoding UTF8 -Force
}

function Get-PortProcess {
    param([int]$Port)
    try {
        $conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($conn) {
            $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
            if ($proc) {
                return @{ pid = $proc.Id; name = $proc.ProcessName; port = $Port }
            }
        }
    } catch {}
    return $null
}

function Show-Status {
    Write-Host "===== 共享设备微运营平台 - 状态检查 =====" -ForegroundColor Cyan
    Write-Host ""

    # SSH 隧道
    $tunnelPid = (Read-Pids).tunnel
    if ($tunnelPid -and (Get-Process -Id $tunnelPid -ErrorAction SilentlyContinue)) {
        Write-Host "[✓] SSH 隧道 (PID: $tunnelPid)" -ForegroundColor Green
    } else {
        Write-Host "[ ] SSH 隧道" -ForegroundColor Yellow
    }

    # 后端
    $backendPid = (Read-Pids).backend
    $backendPort = Get-PortProcess -Port 8080
    if ($backendPort) {
        Write-Host "[✓] 后端服务 :8080 (PID: $($backendPort.pid))" -ForegroundColor Green
    } elseif ($backendPid -and (Get-Process -Id $backendPid -ErrorAction SilentlyContinue)) {
        Write-Host "[ ] 后端服务 (PID: $backendPid, 端口未监听)" -ForegroundColor Yellow
    } else {
        Write-Host "[ ] 后端服务 :8080" -ForegroundColor Red
    }

    # 前端
    $frontendPort = Get-PortProcess -Port 5173
    if ($frontendPort) {
        Write-Host "[✓] 前端开发服务器 :5173 (PID: $($frontendPort.pid))" -ForegroundColor Green
    } else {
        $frontendPort3000 = Get-PortProcess -Port 3000
        if ($frontendPort3000) {
            Write-Host "[✓] 前端开发服务器 :3000 (PID: $($frontendPort3000.pid))" -ForegroundColor Green
        } else {
            Write-Host "[ ] 前端开发服务器" -ForegroundColor Red
        }
    }

    # 远程服务
    Write-Host ""
    Write-Host "--- 远程基础设施 (通过 SSH 隧道) ---" -ForegroundColor Cyan
    $remotePorts = @{15432 = "PostgreSQL/TimescaleDB"; 16379 = "Redis"; 11883 = "Mosquitto"}
    foreach ($port in $remotePorts.Keys) {
        $conn = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($conn -and $conn.State -eq "Established") {
            Write-Host "[✓] $($remotePorts[$port]) 本地 :$port" -ForegroundColor Green
        } elseif ($conn) {
            Write-Host "[~] $($remotePorts[$port]) 本地 :$port ($($conn.State))" -ForegroundColor Yellow
        } else {
            Write-Host "[ ] $($remotePorts[$port]) 本地 :$port" -ForegroundColor Red
        }
    }
}

function Start-Tunnel {
    $pids = Read-Pids
    if ($pids.tunnel -and (Get-Process -Id $pids.tunnel -ErrorAction SilentlyContinue)) {
        Write-Host "SSH 隧道已在运行 (PID: $($pids.tunnel))" -ForegroundColor Yellow
        return
    }

    Write-Host "建立 SSH 隧道..." -ForegroundColor Cyan
    # PostgreSQL:5432→15432, Redis:6379→16379, Mosquitto:1883→11883, 9001→19001
    $sshArgs = @(
        "-N", "-C",
        "-L", "15432:localhost:5432",
        "-L", "16379:localhost:6379",
        "-L", "11883:localhost:1883",
        "-L", "19001:localhost:9001",
        "aliyun2738"
    )

    $null = Start-Process -FilePath "ssh" -ArgumentList $sshArgs -WindowStyle Hidden -PassThru
    Start-Sleep -Seconds 2

    $sshProcs = Get-Process -Name "ssh" -ErrorAction SilentlyContinue | Where-Object {
        $_.StartTime -gt (Get-Date).AddSeconds(-10)
    }
    if ($sshProcs) {
        $procId = $sshProcs[0].Id
        $pids.tunnel = $procId
        Write-Pids $pids
        Write-Host "[✓] SSH 隧道已建立 (PID: $procId)" -ForegroundColor Green
    } else {
        Write-Host "[x] SSH 隧道建立失败" -ForegroundColor Red
    }
}

function Load-EnvFile {
    $envFile = Join-Path $PROJECT_DIR ".env"
    if (Test-Path $envFile) {
        Get-Content $envFile | ForEach-Object {
            if ($_ -match '^\s*([^#=]+)=(.+)\s*$') {
                $key = $matches[1].Trim()
                $val = $matches[2].Trim()
                Set-Item -Path "env:$key" -Value $val -ErrorAction SilentlyContinue
            }
        }
    }
}

function Start-Backend {
    $APP_DIR = Join-Path $BACKEND_DIR "ops-app"
    if (-not (Test-Path $APP_DIR)) {
        Write-Host "后端应用目录不存在: $APP_DIR" -ForegroundColor Red
        return
    }

    $pids = Read-Pids
    if ($pids.backend -and (Get-Process -Id $pids.backend -ErrorAction SilentlyContinue)) {
        Write-Host "后端已在运行 (PID: $($pids.backend))" -ForegroundColor Yellow
        return
    }

    Load-EnvFile

    if (-not $env:JWT_SECRET) {
        $env:JWT_SECRET = "dev-secret-key-please-change-in-production-32chr"
    }
    if (-not $env:DB_PASSWORD) {
        $env:DB_PASSWORD = "iotops123"
    }

    Write-Host "启动后端服务 (ops-app, profile=dev)..." -ForegroundColor Cyan
    Push-Location $APP_DIR
    try {
        $proc = Start-Process -FilePath "mvn" -ArgumentList @("spring-boot:run", "-Dspring-boot.run.profiles=dev") -NoNewWindow -PassThru
        $pids.backend = $proc.Id
        Write-Pids $pids
        Write-Host "[✓] 后端启动中... (PID: $($proc.Id))" -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

function Start-Frontend {
    if (-not (Test-Path $FRONTEND_DIR)) {
        Write-Host "前端目录不存在: $FRONTEND_DIR" -ForegroundColor Red
        return
    }

    Write-Host "启动前端开发服务器..." -ForegroundColor Cyan
    Push-Location $FRONTEND_DIR
    try {
        $proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c pnpm run dev" -NoNewWindow -PassThru
        $pids = Read-Pids
        $pids.frontend = $proc.Id
        Write-Pids $pids
        Write-Host "[✓] 前端启动中... (PID: $($proc.Id))" -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

function Stop-Tunnel {
    $pids = Read-Pids
    if ($pids.tunnel) {
        $proc = Get-Process -Id $pids.tunnel -ErrorAction SilentlyContinue
        if ($proc) {
            $proc | Stop-Process -Force
            Write-Host "[✓] SSH 隧道已停止 (PID: $($pids.tunnel))" -ForegroundColor Green
        }
        $pids.Remove("tunnel")
        Write-Pids $pids
    } else {
        # 尝试找所有 ssh 隧道进程
        $sshProcs = Get-Process -Name "ssh" -ErrorAction SilentlyContinue
        foreach ($p in $sshProcs) {
            if ($p.CommandLine -match "aliyun2738") {
                $p | Stop-Process -Force
                Write-Host "[✓] SSH 隧道已停止 (PID: $($p.Id))" -ForegroundColor Green
            }
        }
    }
}

function Stop-Backend {
    $pids = Read-Pids
    if ($pids.backend) {
        $proc = Get-Process -Id $pids.backend -ErrorAction SilentlyContinue
        if ($proc) {
            $proc | Stop-Process -Force
            Write-Host "[✓] 后端已停止 (PID: $($pids.backend))" -ForegroundColor Green
        }
        $pids.Remove("backend")
        Write-Pids $pids
    }

    # 也尝试找 Java 进程监听 8080
    $javaProcs = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
    foreach ($conn in $javaProcs) {
        $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        if ($proc -and $proc.ProcessName -eq "java") {
            $proc | Stop-Process -Force
            Write-Host "[✓] 后端 Java 进程已停止 (PID: $($proc.Id))" -ForegroundColor Green
        }
    }
}

function Stop-Frontend {
    $pids = Read-Pids
    if ($pids.frontend) {
        $proc = Get-Process -Id $pids.frontend -ErrorAction SilentlyContinue
        if ($proc) {
            $proc | Stop-Process -Force
            Write-Host "[✓] 前端已停止 (PID: $($pids.frontend))" -ForegroundColor Green
        }
        $pids.Remove("frontend")
        Write-Pids $pids
    }

    # 也尝试找 node 进程监听 5173/3000
    foreach ($port in @(5173, 3000)) {
        $conns = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        foreach ($conn in $conns) {
            $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
            if ($proc -and $proc.ProcessName -eq "node") {
                $proc | Stop-Process -Force
                Write-Host "[✓] 前端 Node 进程已停止 (PID: $($proc.Id))" -ForegroundColor Green
            }
        }
    }
}

function Show-Logs {
    param([string]$Component)

    switch ($Component.ToLower()) {
        "backend" {
            $logFile = Join-Path $PROJECT_DIR "src\backend\target\spring.log"
            if (Test-Path $logFile) {
                Get-Content $logFile -Tail 50
            } else {
                Write-Host "后端日志文件未找到" -ForegroundColor Yellow
            }
        }
        "frontend" {
            Write-Host "前端日志输出在控制台窗口中" -ForegroundColor Yellow
        }
        "infra" {
            ssh aliyun2738 "docker compose -f ~/iot-infra/infra-compose.yml logs --tail=50"
        }
        default {
            Write-Host "用法: ./manage.ps1 logs [backend|frontend|infra]" -ForegroundColor Cyan
        }
    }
}

# === 命令路由 ===
switch ($Command) {
    "status"    { Show-Status }
    "tunnel"    { Start-Tunnel }
    "backend"   {
        if ($Target -eq "start") { Start-Backend }
        elseif ($Target -eq "stop") { Stop-Backend }
        elseif ($Target -eq "restart") { Stop-Backend; Start-Sleep 2; Start-Backend }
        elseif ($Target -eq "logs") { Show-Logs -Component "backend" }
        else { Write-Host "用法: ./manage.ps1 backend [start|stop|restart|logs]" -ForegroundColor Cyan }
    }
    "frontend"  {
        if ($Target -eq "start") { Start-Frontend }
        elseif ($Target -eq "stop") { Stop-Frontend }
        elseif ($Target -eq "restart") { Stop-Frontend; Start-Sleep 2; Start-Frontend }
        else { Write-Host "用法: ./manage.ps1 frontend [start|stop|restart]" -ForegroundColor Cyan }
    }
    "start" {
        Start-Tunnel
        Start-Backend
        Start-Frontend
        Write-Host "所有服务启动中..." -ForegroundColor Cyan
        Show-Status
    }
    "stop" {
        Stop-Frontend
        Stop-Backend
        Stop-Tunnel
        Write-Host "所有服务已停止" -ForegroundColor Cyan
    }
    "restart" {
        Stop-Frontend
        Stop-Backend
        Start-Sleep 3
        Start-Tunnel
        Start-Backend
        Start-Frontend
        Write-Host "所有服务重启中..." -ForegroundColor Cyan
    }
    "logs" {
        Show-Logs -Component $Target
    }
    default {
        Write-Host ""
        Write-Host "使用: ./manage.ps1 <command> [target]" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "命令:"
        Write-Host "  status                   检查所有服务状态"
        Write-Host "  start                    启动所有服务（隧道+后端+前端）"
        Write-Host "  stop                     停止所有服务"
        Write-Host "  restart                  重启所有服务"
        Write-Host "  tunnel                   建立 SSH 隧道"
        Write-Host "  backend [start|stop|restart|logs]  管理后端"
        Write-Host "  frontend [start|stop|restart]      管理前端"
        Write-Host "  logs [backend|frontend|infra]      查看日志"
        Write-Host ""
        Write-Host "  PS1 管理脚本会记录各进程 PID，方便生命周期控制。"
        Write-Host "  状态检查自动检测端口占用。"
    }
}
