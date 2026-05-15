function New-Tunnel {
    $p = Get-NetTCPConnection -LocalPort 15432 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($p -and $p.State -eq "Listen") {
        Write-Host "  SSH 隧道已建立" -ForegroundColor Green
        return
    }
    Write-Host "  建立 SSH 隧道 (15432/16379/11883)..." -ForegroundColor Yellow
    ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -f -N `
        -L 15432:localhost:5432 -L 16379:localhost:6379 -L 11883:localhost:1883 aliyun2738 2>$null
    Start-Sleep 2
    try { $s = [System.Net.Sockets.TcpClient]::new(); $s.Connect('127.0.0.1',15432); $s.Close(); Write-Host "  隧道 OK" -ForegroundColor Green }
    catch { Write-Host "  隧道失败" -ForegroundColor Red }
}

function Remove-Tunnel {
    try {
        $procs = ssh -O check aliyun2738 2>&1
        if ($LASTEXITCODE -eq 0) { ssh -O exit aliyun2738 2>$null }
    } catch {}
    $pids = Get-Process -Name "ssh" -ErrorAction SilentlyContinue
    foreach ($p in $pids) { $p | Stop-Process -Force 2>$null }
    Write-Host "  隧道已关闭" -ForegroundColor Yellow
}

function Get-TunnelStatus {
    $ports = @{15432="PG";16379="Redis";11883="MQTT"}
    foreach ($port in $ports.Keys) {
        $p = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($p -and $p.State -eq "Listen") { Write-Host "  $($ports[$port]) :$port OK" -ForegroundColor Green }
        else { Write-Host "  $($ports[$port]) :$port 关闭" -ForegroundColor Red }
    }
}

$cmd = $args[0]
switch ($cmd) {
    "up"    { New-Tunnel }
    "down"  { Remove-Tunnel }
    "restart" { Remove-Tunnel; Start-Sleep 1; New-Tunnel }
    "status" { Get-TunnelStatus }
    default { Write-Host @"
隧道管理: tunnel.ps1 up|down|restart|status
  up      - 建立 SSH 隧道（4 个端口转发）
  down    - 关闭 SSH 隧道
  restart - 重启隧道
  status  - 检查隧道状态
"@ }
}
