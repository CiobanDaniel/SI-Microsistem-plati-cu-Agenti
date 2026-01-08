import json
import requests
import sys
from pydantic import BaseModel, Field
from typing import Optional, Literal

# --- CONFIGURARE ---
# Asigurati-va ca aveti Ollama instalat si ruland (ollama serve)
OLLAMA_BASE_URL = "http://localhost:11434"
OLLAMA_URL = f"{OLLAMA_BASE_URL}/api/generate"

def detect_best_model():
    """Detecteaza automat modelele instalate local si alege unul potrivit."""
    # 0. Daca utilizatorul a specificat un model ca argument (ex: python script.py mistral)
    if len(sys.argv) > 1:
        print(f"[Config] Model fortat manual: {sys.argv[1]}")
        return sys.argv[1]

    try:
        # 1. Interogam API-ul Ollama pentru lista de modele
        resp = requests.get(f"{OLLAMA_BASE_URL}/api/tags", timeout=2)
        if resp.status_code == 200:
            data = resp.json()
            models = [m['name'] for m in data.get('models', [])]
            
            if not models:
                return "llama3" # Fallback
            
            # Cautam modele preferate in ordinea prioritatii
            priorities = ["llama3", "mistral", "gemma", "qwen"]
            for p in priorities:
                for m in models:
                    if p in m: return m
            
            # Daca nu gasim preferate, il returnam pe primul disponibil (ex: tinyllama)
            return models[0]
    except:
        pass
    return "llama3" # Default daca nu raspunde serverul

MODEL_NAME = detect_best_model() 

# --- SCHEME PYDANTIC (Data Validation) ---
class BankOperation(BaseModel):
    intent: Literal["LOGIN", "WITHDRAW", "DEPOSIT", "BALANCE", "UNKNOWN"] = Field(..., description="Tipul operatiunii detectate")
    card_number: Optional[str] = Field(None, description="Numarul cardului daca este mentionat")
    pin: Optional[str] = Field(None, description="Codul PIN daca este mentionat")
    amount: Optional[float] = Field(None, description="Suma tranzactiei daca este mentionata")
    summary: str = Field(..., description="Un scurt rezumat al actiunii")

# --- CORE LOGIC ---
def query_ollama(user_text: str) -> BankOperation:
    system_prompt = """
    Esti un Asistent Bancar Inteligent (Smart Teller) specializat pe limba ROMANA.
    Rolul tau este sa intelegi intentia utilizatorului si sa extragi datele (Card, PIN, Suma) daca exista.
    
    REGULI:
    1. Raspunde DOAR cu JSON valid. Fara markdown, fara explicatii in afara JSON-ului.
    2. Daca utilizatorul intreaba de "sold", "cati bani am", "balanta", intentia este BALANCE.
    3. Daca utilizatorul vrea "sa scoata", "sa retraga", intentia este WITHDRAW.
    4. Daca utilizatorul vrea "sa puna", "sa depuna", intentia este DEPOSIT.
    5. Daca utilizatorul saluta sau zice ceva neclar, intentia este UNKNOWN.

    Exemple:
    User: "Vreau sa scot 100 de lei de pe cardul user1 cu pin 1234"
    JSON: {"intent": "WITHDRAW", "card_number": "user1", "pin": "1234", "amount": 100.0, "summary": "Retragere 100 RON"}
    
    User: "Care e soldul?"
    JSON: {"intent": "BALANCE", "card_number": null, "pin": null, "amount": null, "summary": "Cerere sold"}
    
    User: "Vreau sa vad cati bani am pe cardul test"
    JSON: {"intent": "BALANCE", "card_number": "test", "pin": null, "amount": null, "summary": "Cerere sold pentru card test"}
    """

    prompt = f"{system_prompt}\n\nUser: {user_text}\nJSON:"
    
    payload = {
        "model": MODEL_NAME,
        "prompt": prompt,
        "stream": False,
        "format": "json" # Fortam output JSON (feature Ollama)
    }

    try:
        response = requests.post(OLLAMA_URL, json=payload)
        response.raise_for_status()
        result_text = response.json().get("response", "")
        
        # Validare Pydantic a raspunsului primit de la LLM
        # Asta asigura ca "Agentul" respecta strict contractul de date
        operation_data = json.loads(result_text)
        validated_op = BankOperation(**operation_data)
        
        return validated_op
        
    except requests.exceptions.ConnectionError:
        print(f"\n[EROARE] Nu s-a putut conecta la Ollama la {OLLAMA_URL}.")
        print("Asigurati-va ca ati rulat comanda 'ollama serve' si ati descarcat modelul ('ollama pull llama3').")
        sys.exit(1)
    except Exception as e:
        print(f"\n[EROARE] Procesare esuata: {e}")
        return BankOperation(intent="UNKNOWN", summary="Eroare interna")

# --- INTERFATA CLI ---
def main():
    print("=== Agent Asistent Bancar (Pydantic + Ollama) ===")
    print(f"Model LLM Tinta: {MODEL_NAME}")
    print("Acest agent traduce limbajul natural in comenzi pentru sistemul JADE.")
    print("Scrieti 'exit' pentru a iesi.\n")

    while True:
        user_input = input("\n[Client]: ")
        if user_input.lower() in ["exit", "quit"]:
            break
            
        print(" -> Analizez intentia...")
        op = query_ollama(user_input)
        
        print(f"\n[Agent Pydantic] Interpretare Structurata:")
        print(f"  Intentie: {op.intent}")
        if op.amount: print(f"  Suma:     {op.amount} RON")
        if op.card_number: print(f"  Card:     {op.card_number}")
        print(f"  Rezumat:  {op.summary}")
        
        # Logica conversationala pentru completarea datelor lipsa
        missing_info = []
        
        if op.intent in ["BALANCE", "WITHDRAW", "DEPOSIT", "LOGIN"]:
            if not op.card_number:
                print("\n[Asistent]: Am inteles intentia, dar am nevoie de NUMARUL CARDULUI.")
                op.card_number = input("[Client - Card]: ").strip()
            
            if not op.pin:
                print("\n[Asistent]: Introduceti codul PIN pentru autorizare.")
                op.pin = input("[Client - PIN]: ").strip()
                
        if op.intent in ["WITHDRAW", "DEPOSIT"] and not op.amount:
             print(f"\n[Asistent]: Ce suma doriti sa {('retrageti' if op.intent=='WITHDRAW' else 'depuneti')}?")
             try:
                amt_str = input("[Client - Suma]: ").strip()
                op.amount = float(amt_str)
             except:
                 print("[Asistent]: Suma invalida. Operatie anulata.")
                 continue

        # Generare comanda compatibila JADE (Simulare integrare)
        if op.intent != "UNKNOWN":
            # Construim comanda finala chiar daca datele au venit in etape
            payload_parts = [op.intent, op.card_number, op.pin]
            if op.amount is not None:
                payload_parts.append(str(op.amount))
            else:
                payload_parts.append("0") # Pentru Balance/Login suma e 0 sau ignorata
            
            jade_cmd = ";".join(payload_parts)
            
            print(f"\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
            print(f"[OUTPUT EXECUTOR] Comanda Finala JADE:")
            print(f"'{jade_cmd}'")
            print(f"<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<")
            print("(Copiati aceasta linie in consola agentului JADE sau folositi-o in GUI)")
            
        elif op.intent == "UNKNOWN":
            print("\n[OUTPUT EXECUTOR] Nu am inteles comanda. Va rugam reformulati.")
        else:
             print("\n[INFO] Eroare interna de logica.")

if __name__ == "__main__":
    main()
