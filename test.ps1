$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $root
New-Item -ItemType Directory -Force -Path 'out/test-classes' | Out-Null
$mainSources = Get-ChildItem -Recurse -Filter '*.java' 'src/main/java' | ForEach-Object FullName
$testSources = Get-ChildItem -Recurse -Filter '*.java' 'src/test/java' | ForEach-Object FullName
javac -encoding UTF-8 -d 'out/test-classes' $mainSources $testSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
java -ea -cp 'out/test-classes' ca.yorku.eecs3311.roomscheduler.IntegrationTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

