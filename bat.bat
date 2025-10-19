@echo off
REM ==============================
REM  Configuration
REM ==============================
set "SRC=src\main\java"
set "OUT=out"
set "JAR_NAME=sprint-framework.jar"
set "SERVLET_LIB=C:\Bossy\L3\S5\frameworkS5\lib\servlet-api.jar"
set "JDK_BIN=C:\Users\miojo\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin"

echo ==========================
echo  Compilation du Framework
echo ==========================

REM Créer le dossier de sortie si inexistant
if not exist %OUT% mkdir %OUT%

REM Supprimer anciens .class
del /S /Q %OUT%\*.class >nul 2>&1

REM Compiler FrontServlet + annotations en une seule commande
echo Compilation des fichiers Java...
"%JDK_BIN%\javac.exe" -cp "%SERVLET_LIB%;." -d %OUT% ^
    %SRC%\etu\sprint\framework\*.java ^
    %SRC%\etu\sprint\framework\annotation\*.java

if errorlevel 1 (
    echo.
    echo [ERREUR] La compilation a échoué !
    pause
    exit /b 1
)

echo ==========================
echo  Création du JAR
echo ==========================

cd %OUT%

REM Créer le JAR avec tout le package framework (y compris annotation)
"%JDK_BIN%\jar.exe" cvf %JAR_NAME% etu\sprint\framework >nul

cd ..

REM Déplacer le JAR vers lib/
if not exist lib mkdir lib
move /Y %OUT%\%JAR_NAME% lib\ >nul

echo.
echo [SUCCES] Le JAR %JAR_NAME% est prêt dans lib\
pause
