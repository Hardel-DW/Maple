param(
    [Parameter(Mandatory)] [string] $Name,
    [string] $Simulation = "overstress:idle",
    [int] $PeriodTicks = 100,
    [switch] $HeapDump,
    [switch] $Attach,
    [string[]] $Commands = @()
)

$ErrorActionPreference = "Stop"
$project = Split-Path -Parent $PSScriptRoot
$log = "$project\run\logs\latest.log"

function Send-Rcon([string] $command) {
    $client = New-Object System.Net.Sockets.TcpClient("127.0.0.1", 25575)
    $stream = $client.GetStream()
    $reader = New-Object System.IO.BinaryReader($stream)

    function Send-Packet([int] $id, [int] $type, [string] $body) {
        $bytes = [System.Text.Encoding]::ASCII.GetBytes($body)
        $packet = New-Object System.IO.MemoryStream
        $writer = New-Object System.IO.BinaryWriter($packet)
        $writer.Write([int]($bytes.Length + 10))
        $writer.Write($id)
        $writer.Write($type)
        $writer.Write($bytes)
        $writer.Write([byte]0)
        $writer.Write([byte]0)
        $writer.Flush()
        $stream.Write($packet.ToArray(), 0, [int]$packet.Length)
        $stream.Flush()
        $length = $reader.ReadInt32()
        $null = $reader.ReadInt32()
        $null = $reader.ReadInt32()
        $payload = $reader.ReadBytes($length - 10)
        $null = $reader.ReadBytes(2)
        return [System.Text.Encoding]::ASCII.GetString($payload)
    }

    $null = Send-Packet 1 3 "mapple"
    $response = Send-Packet 2 2 $command
    $client.Close()
    Write-Host "> $command"
    if ($response) { Write-Host "  $response" }
    return $response
}

function Wait-Log([string] $pattern, [int] $timeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if ((Test-Path $log) -and (Select-String -Path $log -Pattern $pattern -Quiet)) { return }
        Start-Sleep -Seconds 5
    }
    throw "Timed out waiting for '$pattern'"
}

if (-not $Attach) {
    if (Test-Path $log) { Remove-Item $log }
    $server = Start-Process -FilePath "$project\gradlew.bat" -ArgumentList "runServer", "--offline", "-q" -WorkingDirectory $project -PassThru -WindowStyle Hidden
    Write-Host "Server starting, pid $($server.Id)"
    Wait-Log 'Done \(' 600
    Wait-Log 'RCON running' 60
}

$started = Send-Rcon "overstress simulation start $Simulation"
$durationTicks = [int]([regex]::Match($started, 'for (\d+) ticks').Groups[1].Value)
$null = Send-Rcon "mapple metrics run start $Name $PeriodTicks"
Start-Sleep -Seconds 60
foreach ($command in $Commands) { $null = Send-Rcon $command }
Start-Sleep -Seconds ($durationTicks / 20 - 90)
if ($HeapDump) { $null = Send-Rcon "mapple metrics heapdump" }
$closed = Send-Rcon "mapple metrics run stop"
$runDirectory = Join-Path $project "run" | Join-Path -ChildPath ([regex]::Match($closed, 'Run closed at (.+)$').Groups[1].Value)
Wait-Log "Simulation $Simulation stopped" 600
$null = Send-Rcon "stop"
if (-not $Attach) { $server.WaitForExit() } else { Wait-Log "All dimensions are saved" 300 }
Copy-Item $log (Join-Path $runDirectory "server.log")
Write-Host "Run $Name complete"
