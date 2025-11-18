@echo off
REM run_client.bat - 클라이언트 실행 (out 폴더가 준비되어 있어야 함)
:: 프로젝트 루트에서 실행하거나 이 파일이 있는 위치에서 실행하세요.
if not exist out (
    echo "out 폴더가 없습니다. 먼저 build.bat를 실행하여 컴파일하세요."
    pause
    exit /b 1
)
echo Starting OmokClient...
java -cp out OmokClient
pause
