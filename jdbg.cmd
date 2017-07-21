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
FOR /F "delims=" %%X IN ('WHERE javac.exe') DO (
	SET JDK_PATH=%%~dpX
	IF "!JDK_PATH:~-1!" == "\" (
		SET JDK_PATH=!JDK_PATH:~0,-1!
	)
	FOR /F "delims=; tokens=1,*" %%Y IN ("!JDK_PATH!") DO (
		SET JDK_PATH=%%~dpY
	)
	IF "!JDK_PATH:~-1!" == "\" (
		SET JDK_PATH=!JDK_PATH:~0,-1!
	)
)

SETLOCAL DISABLEDELAYEDEXPANSION

:LOAD
PUSHD %CD%

javac.exe -classpath "%JDK_PATH%\lib\tools.jar" %~dp0src\*.java
IF NOT %ERRORLEVEL% == 0 (
	GOTO EOF
)
CALL java.exe -classpath "%JDK_PATH%\lib\tools.jar" com.sun.tools.example.debug.tty.TTY %*

POPD
GOTO EOF

:ABORT
PAUSE
EXIT /B 1

:EOF
EXIT /B %ERRORLEVEL%
