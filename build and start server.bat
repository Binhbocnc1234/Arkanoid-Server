@echo off

REM Kill old server on port 54555 if exists
for /f "tokens=5" %%a in ('netstat -a -n -o ^| findstr 54555') do taskkill /PID %%a /F

REM Compile và run server, giữ terminal mở
start "" cmd /k "mvn compile && mvn exec:java"

REM Delay 3 giây để server kịp start
timeout /t 3 >nul

REM Chạy ngrok TCP tunnel cho port 54555, giữ terminal mở
start "" cmd /k "ngrok tcp 54555"
