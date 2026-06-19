@echo off
:: Cài Gateway chạy ngầm như Windows Service (dùng Task Scheduler - SYSTEM account)
:: Chạy file này với quyền Administrator
:: Tu dong chay khi boot, khong can user login, tu restart neu crash

setlocal EnableDelayedExpansion
set TASK_NAME=MuaSamCongGateway
set EXE_PATH=%~dp0gateway.exe
set WORK_DIR=%~dp0

echo ============================================
echo   MuaSamCong Gateway - Cai Dat Service
echo ============================================
echo.

:: Kiem tra quyen Administrator
net session >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Hay chuot phai file nay va chon "Run as Administrator"
    pause
    exit /b 1
)

:: Kiem tra gateway.exe ton tai
if not exist "%EXE_PATH%" (
    echo [LOI] Khong tim thay: %EXE_PATH%
    pause
    exit /b 1
)

echo [1/4] Xoa task cu neu co...
schtasks /delete /tn "%TASK_NAME%" /f >nul 2>&1

echo [2/4] Tao Scheduled Task (chay voi SYSTEM account, tu dong khi boot)...

:: Tao XML config cho task
set XML_PATH=%TEMP%\gateway-task.xml
(
echo ^<?xml version="1.0" encoding="UTF-16"?^>
echo ^<Task version="1.2" xmlns="http://schemas.microsoft.com/windows/2004/02/mit/task"^>
echo   ^<RegistrationInfo^>
echo     ^<Description^>MuaSamCong File Download Gateway^</Description^>
echo   ^</RegistrationInfo^>
echo   ^<Triggers^>
echo     ^<BootTrigger^>
echo       ^<Enabled^>true^</Enabled^>
echo       ^<Delay^>PT5S^</Delay^>
echo     ^</BootTrigger^>
echo   ^</Triggers^>
echo   ^<Principals^>
echo     ^<Principal id="Author"^>
echo       ^<UserId^>S-1-5-18^</UserId^>
echo       ^<RunLevel^>HighestAvailable^</RunLevel^>
echo     ^</Principal^>
echo   ^</Principals^>
echo   ^<Settings^>
echo     ^<MultipleInstancesPolicy^>IgnoreNew^</MultipleInstancesPolicy^>
echo     ^<DisallowStartIfOnBatteries^>false^</DisallowStartIfOnBatteries^>
echo     ^<StopIfGoingOnBatteries^>false^</StopIfGoingOnBatteries^>
echo     ^<ExecutionTimeLimit^>PT0S^</ExecutionTimeLimit^>
echo     ^<RestartOnFailure^>
echo       ^<Interval^>PT1M^</Interval^>
echo       ^<Count^>10^</Count^>
echo     ^</RestartOnFailure^>
echo     ^<Enabled^>true^</Enabled^>
echo   ^</Settings^>
echo   ^<Actions Context="Author"^>
echo     ^<Exec^>
echo       ^<Command^>%EXE_PATH%^</Command^>
echo       ^<WorkingDirectory^>%WORK_DIR%^</WorkingDirectory^>
echo     ^</Exec^>
echo   ^</Actions^>
echo ^</Task^>
) > "%XML_PATH%"

schtasks /create /tn "%TASK_NAME%" /xml "%XML_PATH%" /f
del "%XML_PATH%" >nul 2>&1

if %ERRORLEVEL% NEQ 0 (
    echo [LOI] Khong the tao Scheduled Task.
    pause
    exit /b 1
)

echo [3/4] Khoi dong gateway ngay bay gio...
schtasks /run /tn "%TASK_NAME%"
timeout /t 3 /nobreak >nul

echo [4/4] Kiem tra trang thai...
schtasks /query /tn "%TASK_NAME%" /fo LIST | findstr /i "Status Last Run"

echo.
echo ============================================
echo  Da cai dat thanh cong!
echo.
echo  Task: %TASK_NAME%
echo  EXE : %EXE_PATH%
echo  - Tu dong chay khi bat may (5 giay sau boot)
echo  - Chay ngam, khong hien cua so
echo  - Tu restart neu bi loi (toi da 10 lan)
echo  - Chay voi quyen SYSTEM (khong can login)
echo ============================================
echo.
echo Quan ly:
echo   Start : schtasks /run /tn "%TASK_NAME%"
echo   Stop  : schtasks /end /tn "%TASK_NAME%"
echo   Status: schtasks /query /tn "%TASK_NAME%"
pause
