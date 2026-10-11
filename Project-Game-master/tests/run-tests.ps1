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
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.FireStaffAnimationTest
if ($LASTEXITCODE -ne 0) { throw 'Fire Staff animation checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.ExplosionEffectTest
if ($LASTEXITCODE -ne 0) { throw 'Explosion effect checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.LevelUpOptionsTest
if ($LASTEXITCODE -ne 0) { throw 'Level-up option checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.LevelUpEffectTest
if ($LASTEXITCODE -ne 0) { throw 'Level-up animation checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.SwordOfJusticeTest
if ($LASTEXITCODE -ne 0) { throw 'Sword of Justice checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.FrostHammerTest
if ($LASTEXITCODE -ne 0) { throw 'Frost Hammer checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.TornadoTest
if ($LASTEXITCODE -ne 0) { throw 'Tornado checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.WeaponScalingTest
if ($LASTEXITCODE -ne 0) { throw 'Weapon scaling checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.LaserTravelTest
if ($LASTEXITCODE -ne 0) { throw 'Laser travel checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.BossDeathEffectTest
if ($LASTEXITCODE -ne 0) { throw 'Boss death effect checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.EnemyMeleeAttackTest
if ($LASTEXITCODE -ne 0) { throw 'Enemy melee attack checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.ContactDamageTest
if ($LASTEXITCODE -ne 0) { throw 'Contact damage checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.StrongBoyCharacterTest
if ($LASTEXITCODE -ne 0) { throw 'Strong Boy character checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.MapSelectionTest
if ($LASTEXITCODE -ne 0) { throw 'Map selection checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.PauseMenuTest
if ($LASTEXITCODE -ne 0) { throw 'Pause menu checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.BossVictoryTest
if ($LASTEXITCODE -ne 0) { throw 'Boss victory checks failed' }
& java '-Djava.awt.headless=true' '--class-path' "$compileDir;$sourceDir" project.game.GunCollisionTest
if ($LASTEXITCODE -ne 0) { throw 'Gun collision checks failed' }
