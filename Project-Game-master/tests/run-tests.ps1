$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$compileDir = Join-Path $env:TEMP ('game-regression-' + [guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $compileDir | Out-Null
$sourceDir = Join-Path $projectRoot 'src'
$javaSources = @(Get-ChildItem -LiteralPath (Join-Path $sourceDir 'project/game') -Filter '*.java' | ForEach-Object { $_.FullName })
$javaSources += @(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java' | ForEach-Object { $_.FullName })
& javac -encoding UTF-8 --release 23 -d $compileDir $javaSources
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.GameRegressionTest
if ($LASTEXITCODE -ne 0) { throw 'Regression checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.BossCombatTest
if ($LASTEXITCODE -ne 0) { throw 'Boss combat checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.SpawnBalanceTest
if ($LASTEXITCODE -ne 0) { throw 'Spawn balance checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.ExplosionEffectTest
if ($LASTEXITCODE -ne 0) { throw 'Explosion effect checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.LevelUpOptionsTest
if ($LASTEXITCODE -ne 0) { throw 'Level-up option checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.WeaponScalingTest
if ($LASTEXITCODE -ne 0) { throw 'Weapon scaling checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.BossDeathEffectTest
if ($LASTEXITCODE -ne 0) { throw 'Boss death effect checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.ContactDamageTest
if ($LASTEXITCODE -ne 0) { throw 'Contact damage checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.StrongBoyCharacterTest
if ($LASTEXITCODE -ne 0) { throw 'Strong Boy character checks failed' }
