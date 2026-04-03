@echo off
chcp 65001 >nul
echo ========================================
echo  Building hanai and copying to phone_claw
echo ========================================

echo.
echo [1/3] Building hanai jar...
cd /d D:\WorkSpace\src\hanai
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)

echo.
echo [2/3] Copying jar to phone_claw...
copy /Y "target\hanai-1.0-SNAPSHOT.jar" "D:\WorkSpace\src\phone_claw\app\libs\hanai-1.0-SNAPSHOT.jar"
if %errorlevel% neq 0 (
    echo ERROR: Copy failed!
    pause
    exit /b 1
)

echo.
echo [3/3] Building phone_claw...
cd /d D:\WorkSpace\src\phone_claw
call gradlew.bat assembleDebug
if %errorlevel% neq 0 (
    echo ERROR: Gradle build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo  Build completed successfully!
echo ========================================
pause
