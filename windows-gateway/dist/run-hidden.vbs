' Chạy gateway.exe không hiện cửa sổ CMD
' Double-click file này để start gateway ngầm

Dim scriptDir
scriptDir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)

Dim exePath
exePath = scriptDir & "\gateway.exe"

Dim wsh
Set wsh = CreateObject("WScript.Shell")

' WindowStyle 0 = ẩn hoàn toàn
wsh.Run Chr(34) & exePath & Chr(34), 0, False

WScript.Echo "Gateway đã chạy ngầm trên cổng 18080."
