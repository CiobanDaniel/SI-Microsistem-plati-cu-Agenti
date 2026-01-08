@echo off
SETLOCAL

cd python_agent || (
    echo [EROARE] Nu pot intra in folderul python_agent.
    pause
    exit /b 1
)

REM Verificam daca Python este instalat
python --version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [EROARE] Python nu este instalat sau nu este in PATH.
    echo Va rugam instalati Python de la python.org.
    pause
    exit /b 1
)

REM Verificam/Cream mediul virtual
IF NOT EXIST ".venv" (
    echo [SETUP] Se creeaza mediul virtual Python...
    python -m venv .venv
    
    echo [SETUP] Se activeaza mediul...
    call .venv\Scripts\activate
    
    echo [SETUP] Se instaleaza dependintele...
    pip install -r requirements.txt || (
        echo [EROARE] Instalarea dependentelor a esuat.
        pause
        exit /b 1
    )
) ELSE (
    echo [INFO] Mediul virtual existent detectat. Se activeaza...
    call .venv\Scripts\activate
)

REM Rulare
echo.
echo [INFO] Se porneste Agentul Asistent...
echo Asigurati-va ca Ollama ruleaza in alt terminal ('ollama serve')!
echo.
python financial_assistant.py

echo.
echo [INFO] Scriptul s-a incheiat.
pause
