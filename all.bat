cd /d %~dp0
cd ../AQS 
call aqs.bat
cd ../
cd note
call note.bat
cd ../
mshta vbscript:msgbox("ÌáÊ¾ÄÚÈÝ1",1,"ÌáÊ¾´°¿Ú1")(window.close)