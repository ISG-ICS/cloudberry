set varX=D:\
for %%i in (1,1,80) do (
set /a varX=D:\latex
set xxx = "hello world" 
echo !varX!
cd !%varX%!
)
pause