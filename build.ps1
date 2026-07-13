$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
New-Item -ItemType Directory -Force -Path 'out/classes' | Out-Null
$sources = Get-ChildItem -Recurse -Filter '*.java' 'src/main/java' | ForEach-Object FullName
if (-not $sources) { throw 'No Java sources found.' }
javac -encoding UTF-8 -d 'out/classes' $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Build successful: out/classes'

