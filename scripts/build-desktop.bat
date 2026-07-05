@echo off
setlocal

set "APP_DIR=%~dp0.."
cd /d "%APP_DIR%"

echo === Compilando frontend ===
call npm --prefix frontend run build
if errorlevel 1 goto error

echo === Copiando frontend al backend ===
if exist src\main\resources\static rmdir /s /q src\main\resources\static
mkdir src\main\resources\static
xcopy /e /i /y frontend\dist\* src\main\resources\static\ >nul

echo === Empaquetando backend (mvn package) ===
call mvn -q clean package
if errorlevel 1 goto error

echo.
echo Listo. Usa scripts\iniciar-gastos.bat para arrancar la app.
pause
exit /b 0

:error
echo.
echo Hubo un error en el build. Revisa el mensaje de arriba.
pause
exit /b 1
