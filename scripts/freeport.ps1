param([int]$Port=8080)
$conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
if ($conn) {
    $procId = $conn.OwningProcess
    Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
    Write-Output "Killed PID $procId on port $Port"
    Start-Sleep 2
}
$check = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
if (-not $check) { Write-Output "PORT_FREE" } else { Write-Output "STILL_IN_USE" }
