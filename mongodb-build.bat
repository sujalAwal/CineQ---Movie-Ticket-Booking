@echo off
echo ========================================
echo MongoDB Migration - Clean Build Script
echo ========================================
echo.

echo [Step 1/3] Cleaning target directory...
if exist target (
    rmdir /s /q target
    echo Target directory deleted
) else (
    echo No target directory found
)
echo.

echo [Step 2/3] Cleaning Maven cache...
call mvnw clean
echo.

echo [Step 3/3] Compiling project...
call mvnw compile -DskipTests
echo.

echo ========================================
echo Build Complete!
echo ========================================
echo.
echo If there are no errors above, your MongoDB migration is successful!
echo.
pause

