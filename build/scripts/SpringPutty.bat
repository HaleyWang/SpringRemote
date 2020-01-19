@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  SpringPutty startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and SPRING_PUTTY_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\SpringRemote-0.1.jar;%APP_HOME%\lib\eawtstub.jar;%APP_HOME%\lib\cookswing.jar;%APP_HOME%\lib\cookxml.jar;%APP_HOME%\lib\pty4j-0.8.5.jar;%APP_HOME%\lib\slf4j-log4j12-1.7.26.jar;%APP_HOME%\lib\log4j-1.2.17.jar;%APP_HOME%\lib\kotlin-stdlib-1.2.51.jar;%APP_HOME%\lib\annotations-16.0.2.jar;%APP_HOME%\lib\jna-platform-4.5.0.jar;%APP_HOME%\lib\jna-4.5.0.jar;%APP_HOME%\lib\trove4j.jar;%APP_HOME%\lib\jsch-0.1.54.jar;%APP_HOME%\lib\jzlib-1.1.3.jar;%APP_HOME%\lib\guava-25.1-jre.jar;%APP_HOME%\lib\gson-2.8.5.jar;%APP_HOME%\lib\flatlaf-0.22.jar;%APP_HOME%\lib\swingx-1.6.1.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.2.51.jar;%APP_HOME%\lib\purejavacomm-0.0.11.1.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\checker-qual-2.0.0.jar;%APP_HOME%\lib\error_prone_annotations-2.1.3.jar;%APP_HOME%\lib\j2objc-annotations-1.1.jar;%APP_HOME%\lib\animal-sniffer-annotations-1.14.jar;%APP_HOME%\lib\slf4j-api-1.7.26.jar;%APP_HOME%\lib\filters-2.0.235.jar;%APP_HOME%\lib\swing-worker-1.1.jar

@rem Execute SpringPutty
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SPRING_PUTTY_OPTS%  -classpath "%CLASSPATH%" com.haleywang.putty.SpringRemote %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable SPRING_PUTTY_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%SPRING_PUTTY_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
