@echo off
:: Go cai dat Gateway service
:: Chay voi quyen Administrator

setlocal
set TASK_NAME=MuaSamCongGateway

echo [1/2] Dung gateway...
schtasks /end /tn "%TASK_NAME%" >nul 2>&1
timeout /t 2 /nobreak >nul

echo [2/2] Xoa task...
schtasks /delete /tn "%TASK_NAME%" /f

echo.
echo Da go cai dat "%TASK_NAME%".
pause
