@echo off
setlocal enabledelayedexpansion

for /f "tokens=2 delims==" %%V in ('findstr "^mod_version=" gradle.properties') do set MOD_VERSION=%%V
for /f "tokens=2 delims==" %%V in ('findstr "^archives_base_name=" gradle.properties') do set BASE_NAME=%%V

set GRADLE=
where gradle >nul 2>&1
if %errorlevel% == 0 (
    set GRADLE=gradle
) else (
    for /f "delims=" %%G in ('where /r "%USERPROFILE%\.gradle\wrapper\dists" gradle.bat 2^>nul') do set GRADLE=%%G
)
if not defined GRADLE (
    echo ERROR: No Gradle binary found. Install Gradle or add it to PATH.
    exit /b 1
)

if not exist dist mkdir dist

for %%T in (1.21.x 26.1.x) do (
    echo === Building %%T ===
    call "!GRADLE!" clean build -Pmc_target=%%T --no-daemon -q
    if errorlevel 1 ( echo Build failed for %%T & exit /b 1 )
    copy /y "build\libs\%BASE_NAME%-%MOD_VERSION%.jar" "dist\%BASE_NAME%-%MOD_VERSION%+%%T.jar" >nul
    echo -^> dist\%BASE_NAME%-%MOD_VERSION%+%%T.jar
)

echo.
echo Done. 2 JARs written to dist\
