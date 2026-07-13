$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
& "$root/build.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Set-Location $root
java -cp 'out/classes' ca.yorku.eecs3311.roomscheduler.App

