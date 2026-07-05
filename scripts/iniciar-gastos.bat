@echo off
setlocal

set "APP_DIR=%~dp0.."
set "JAR=%APP_DIR%\target\gastos-0.0.1-SNAPSHOT.jar"
set "LOG=%APP_DIR%\gastos.log"

if not exist "%JAR%" (
    mshta "javascript:alert('No se encontro el jar de Gastos. Corre scripts\build-desktop.bat primero.');close();"
    exit /b 1
)

start "" javaw -XX:TieredStopAtLevel=1 -Xshare:auto -Dspring.profiles.active=local -jar "%JAR%" >> "%LOG%" 2>&1

REM Espera a que el backend levante antes de abrir el navegador
:esperar
timeout /t 1 /nobreak >nul
set "STATUS="
curl -s -o nul -w "%%{http_code}" http://localhost:8080/ > "%TEMP%\gastos-status.txt" 2>nul
set /p STATUS=<"%TEMP%\gastos-status.txt"
if not "%STATUS%"=="200" goto esperar

start "" http://localhost:8080/
