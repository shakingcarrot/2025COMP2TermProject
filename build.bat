@echo off
REM build.bat - 컴파일을 수행하여 out 폴더에 클래스 파일 생성










pause)    echo Build failed. Check compiler output.) else (    echo Build completed successfully.if %ERRORLEVEL% equ 0 (javac -d out OmokServer\*.java OmokClient\*.javaif not exist out mkdir out:: 프로젝트 루트에서 실행하세요 (이 파일이 프로젝트 루트에 있어야 함)