@ECHO OFF

SETLOCAL
SETLOCAL ENABLEEXTENSIONS
SETLOCAL ENABLEDELAYEDEXPANSION

IF EXIST "%~f0" (
	IF NOT "%~x0" == "" (
		GOTO ENTRYPOINT
	)
)

SET PATH_CDR=%CD%;%PATH%
:FINDNEXT
FOR /F "delims=; tokens=1,*" %%P IN ("!PATH_CDR!") DO (
	SET PATH_CAR=%%P
	IF "!PATH_CAR:~-1!" == "\" (
		SET PATH_CAR=!PATH_CAR:~0,-1!
	)
	IF NOT "!PATH_CAR!" == "" (
		FOR %%X IN (%PATHEXT:;=;%) DO (
			IF EXIST "!PATH_CAR!\%~n0%%X" (
				SET PATH_CAR=!PATH_CAR!\%~n0%%X
				GOTO TRAMPOLINE
			)
		)
	)
	SET PATH_CDR=%%Q
	IF DEFINED PATH_CDR (
		GOTO FINDNEXT
	)
)
ECHO Error: failed to detect batch file path 1>&2
EXIT /B -1

:TRAMPOLINE
CALL "!PATH_CAR!" %*
EXIT /B !ERRORLEVEL!

:ENTRYPOINT
SETLOCAL DISABLEDELAYEDEXPANSION

IF "%JAVA_HOME%" == "" (
	ECHO Error: JDK not found, make sure you set the JAVA_HOME environment variable 1>&2
	EXIT /B -1
)

:LOAD
PUSHD %CD%

:ALLOC
SET OUTPUT_DIR=%TEMP%\jdbg-%RANDOM%
IF EXIST "%OUTPUT_DIR%" (
	GOTO ALLOC
)
MKDIR "%OUTPUT_DIR%" 2> NUL
IF NOT %ERRORLEVEL% == 0 (
	GOTO ALLOC
)

"%JAVA_HOME%\bin\javac.exe" -classpath "%JAVA_HOME%\lib\tools.jar" -d "%OUTPUT_DIR%" "%~dp0src"\*.java "%~dp0src"\command\*.java
IF NOT %ERRORLEVEL% == 0 (
	GOTO EOF
)

"%JAVA_HOME%\bin\java.exe" -classpath "%OUTPUT_DIR%";"%JAVA_HOME%\lib\tools.jar" jdbg.MainClass %*

RMDIR /S /Q "%OUTPUT_DIR%"

POPD

:EOF
EXIT /B %ERRORLEVEL%
