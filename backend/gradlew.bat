@ECHO OFF
SET DIR=%~dp0
SET APP_HOME=%DIR:~0,-1%

IF NOT "%JAVA_HOME%"=="" (
  SET JAVA_EXE=%JAVA_HOME%\bin\java.exe
) ELSE (
  SET JAVA_EXE=java
)

IF NOT EXIST "%JAVA_EXE%" (
  ECHO ERROR: Java executable not found. Please install Java 21 or set JAVA_HOME.
  EXIT /B 1
)

SET WRAPPER_JAR=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
IF NOT EXIST "%WRAPPER_JAR%" (
  IF NOT EXIST "%APP_HOME%\gradle\wrapper" (
    mkdir "%APP_HOME%\gradle\wrapper"
  )
  powershell -Command "Invoke-WebRequest -Uri https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar -OutFile '%WRAPPER_JAR%'"
)

"%JAVA_EXE%" -Xms64m -Xmx512m -classpath "%WRAPPER_JAR%" org.gradle.wrapper.GradleWrapperMain %*
