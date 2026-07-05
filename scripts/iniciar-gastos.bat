@echo off
setlocal

set "APP_DIR=%~dp0.."
set "JAR=%APP_DIR%\target\gastos-0.0.1-SNAPSHOT.jar"

if not exist "%JAR%" (
    echo No se encontro %JAR%
    echo Corre primero scripts\build-desktop.bat para compilar la app.
    pause
    exit /b 1
)

title Gastos
echo Iniciando Gastos...
start "" /min java -Dspring.profiles.active=local -jar "%JAR%"

REM Espera a que el backend levante antes de abrir el navegador
:esperar
timeout /t 1 /nobreak >nul
curl -s -o nul -w "%%{http_code}" http://localhost:8080/ > "%TEMP%\gastos-status.txt" 2>nul
set /p STATUS=<"%TEMP%\gastos-status.txt"
if not "%STATUS%"=="200" goto esperar

start "" http://localhost:8080/
