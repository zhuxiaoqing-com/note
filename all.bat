cd /d %~dp0
cd ../AQS 
call aqs.bat
cd ../
cd note
call note.bat
cd ../
mshta vbscript:msgbox("提示内容1",1,"提示窗口1")(window.close)