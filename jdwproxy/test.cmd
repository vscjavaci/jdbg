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
SET SAVED_JAVA_OPTS=%JAVA_OPTS%
SET JAVA_OPTS=-Djava.net.preferIPv4Stack=true -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=127.0.0.1:8898
START "debugee" jdwproxy
SET JAVA_OPTS=%SAVED_JAVA_OPTS%
START "stub" jdwproxy stub 127.0.0.1:8888 127.0.0.1:8898
START "proxy" jdwproxy proxy 127.0.0.1:7777 127.0.0.1:8888
..\jdbg\jdbg -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=7777
POPD

:EOF
EXIT /B %ERRORLEVEL%
