#!/bin/bash

# Mergem in folderul agentului
cd python_agent || exit

# Verificam Python 3
if ! command -v python3 &> /dev/null; then
    echo "[EROARE] Python 3 nu este instalat."
    exit 1
fi

# Configurare venv
if [ ! -d ".venv" ]; then
    echo "[SETUP] Se creeaza mediul virtual Python..."
    python3 -m venv .venv
    
    echo "[SETUP] Activare si instalare dependinte..."
    source .venv/bin/activate
    pip install -r requirements.txt
else
    echo "[INFO] Activare mediu existent..."
    source .venv/bin/activate
fi

# Rulare
echo ""
echo "[INFO] Pornire Asistent Inteligenta..."
echo "Nota: Asigura-te ca Ollama ruleaza ('ollama serve')!"
python financial_assistant.py
