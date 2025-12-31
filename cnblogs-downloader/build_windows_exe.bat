@echo off
echo Building Cnblogs Downloader for Windows...

rem Check if PyInstaller is installed
pip show pyinstaller >nul 2>&1
if %errorlevel% neq 0 (
    echo PyInstaller not found. Installing...
    pip install pyinstaller
)

rem Check if other requirements are installed
pip install -r requirements.txt

echo Running PyInstaller...
pyinstaller cnblogs_downloader.spec --clean

if %errorlevel% equ 0 (
    echo.
    echo Build successful!
    echo executable is located in the dist folder.
    dir dist\*.exe
) else (
    echo.
    echo Build failed!
)

pause
