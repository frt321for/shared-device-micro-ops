function New-Tunnel {
    $p = Get-NetTCPConnection -LocalPort 15432 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($p -and $p.State -eq "Listen") { Write-Host "  SSH tunnel OK" -ForegroundColor Green; return }
    Write-Host "  Starting SSH tunnel (15432/16379/11883)..." -ForegroundColor Yellow
    ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o ExitOnForwardFailure=yes -f -N `
        -L 15432:localhost:5432 -L 16379:localhost:6379 -L 11883:localhost:1883 aliyun2738 2>$null
    Start-Sleep 2
    try { $s = [System.Net.Sockets.TcpClient]::new(); $s.Connect('127.0.0.1',15432); $s.Close(); Write-Host "  Tunnel OK" -ForegroundColor Green }
    catch { Write-Host "  Tunnel FAILED" -ForegroundColor Red }
}
function Remove-Tunnel {
    try { $procs = ssh -O check aliyun2738 2>&1; if ($LASTEXITCODE -eq 0) { ssh -O exit aliyun2738 2>$null } } catch {}
    Get-Process -Name "ssh" -ErrorAction SilentlyContinue | Stop-Process -Force 2>$null
    Write-Host "  Tunnel closed" -ForegroundColor Yellow
}
function Get-TunnelStatus {
    $ports = @{15432="PG";16379="Redis";11883="MQTT"}
    foreach ($port in $ports.Keys) {
        $p = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($p -and $p.State -eq "Listen") { Write-Host "  $($ports[$port]) :$port OK" -ForegroundColor Green }
        else { Write-Host "  $($ports[$port]) :$port DOWN" -ForegroundColor Red }
    }
}
$cmd = $args[0]
switch ($cmd) {
    "up"    { New-Tunnel }
    "down"  { Remove-Tunnel }
    "restart" { Remove-Tunnel; Start-Sleep 1; New-Tunnel }
    "status" { Get-TunnelStatus }
    default {
        Write-Host "`nTunnel mgmt: tunnel.ps1 up|down|restart|status"
        Write-Host "  up      - Establish SSH tunnel (4 fwd)"
        Write-Host "  down    - Close"
        Write-Host "  restart - Restart"
        Write-Host "  status  - Check status"
    }
}
