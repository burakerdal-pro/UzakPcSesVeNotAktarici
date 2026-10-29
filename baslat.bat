@echo off
title PC Not Defteri Sunucusu
cd /d "%~dp0"
echo ============================================================
echo       PC NOT DEFTERI SUNUCUSU BASLATILIYOR...
echo ============================================================
python pc_server.py
if errorlevel 1 (
    echo.
    echo Python komutu bulunamadi, 'py' komutu deneniyor...
    py pc_server.py
)
pause
