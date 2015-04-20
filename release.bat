@ECHO OFF

SET WD=%CD%
SET SD=%~dp0
SET PARAMS=%*

cd "%SD%"

call mvn release:clean release:prepare %PARAMS%
call mvn release:perform %PARAMS%

cd "%WD%"

PAUSE
