set SRC_DIR=..\src\main\proto
set DST_DIR=..\gen
protoc-3.1.0-windows-x86_64.exe -I=%SRC_DIR% --grpc_out=%DST_DIR% --java_out=%DST_DIR% %SRC_DIR%\Neela.proto
