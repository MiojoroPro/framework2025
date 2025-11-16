@echo off
REM ==============================
REM  Configuration
REM ==============================
set "SRC=src\main\java"
set "OUT=out"
set "JAR_NAME=sprint-framework.jar"
set "SERVLET_LIB=C:\Bossy\L3\S5\frameworkS5\lib\servlet-api.jar"
set "JDK_BIN=C:\Users\miojo\AppData\Local\Programs\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin"
set "TOMCAT_CLASSES=C:\Bossy\apache-tomcat-9.0.109\webapps\Framework-Test\WEB-INF\classes"

echo ==========================
echo  Compilation du Framework
echo ==========================

REM Créer le dossier de sortie si inexistant
if not exist %OUT% mkdir %OUT%

REM Supprimer anciens .class
del /S /Q %OUT%\*.class >nul 2>&1

REM Compiler le framework
echo Compilation des fichiers Java du framework...
"%JDK_BIN%\javac.exe" -cp "%SERVLET_LIB%;." -d %OUT% ^
    %SRC%\etu\sprint\framework\*.java ^
    %SRC%\etu\sprint\framework\annotation\*.java ^
    %SRC%\etu\sprint\framework\controller\*.java

if errorlevel 1 (
    echo.
    echo [ERREUR] La compilation du framework a échoué !
    pause
    exit /b 1
)

echo ==========================
echo  Création du JAR du framework
echo ==========================

cd %OUT%
"%JDK_BIN%\jar.exe" cvf %JAR_NAME% etu\sprint\framework >nul
cd ..

REM Déplacer le JAR vers lib/
if not exist lib mkdir lib
move /Y %OUT%\%JAR_NAME% lib\ >nul

echo.
echo [SUCCES] Le JAR %JAR_NAME% est prêt dans lib\


echo ==========================
echo  Compilation des contrôleurs de test
echo ==========================

REM Créer le dossier de destination sur Tomcat s'il n'existe pas
if not exist "%TOMCAT_CLASSES%\etu\test\controller" mkdir "%TOMCAT_CLASSES%\etu\test\controller"

REM Compiler tous les fichiers .java dans etu.test.controller
for %%f in (%SRC%\etu\test\controller\*.java) do (
    echo Compilation de %%~nxf ...
    "%JDK_BIN%\javac.exe" -cp "%SERVLET_LIB%;lib\sprint-framework.jar;." -d "%TOMCAT_CLASSES%" "%%f"
    if errorlevel 1 (
        echo.
        echo [ERREUR] La compilation du contrôleur %%~nxf a échoué !
        pause
        exit /b 1
    )
)

echo.
echo [SUCCES] Tous les contrôleurs compilés ont été copiés dans :
echo   %TOMCAT_CLASSES%\etu\test\controller
echo.

REM Copier le JAR dans Tomcat WEB-INF/lib
if not exist "%TOMCAT_CLASSES%\..\lib" mkdir "%TOMCAT_CLASSES%\..\lib"
copy /Y lib\sprint-framework.jar "%TOMCAT_CLASSES%\..\lib\"
echo [SUCCES] Le JAR sprint-framework.jar a été copié dans Tomcat WEB-INF/lib


pause

