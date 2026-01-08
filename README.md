# Sistem Bancar Multi-Agent cu JADE

Acest proiect reprezintă o simulare a unui sistem bancar distribuit, implementat folosind platforma Multi-Agent JADE (Java Agent DEvelopment Framework). Sistemul modelează interacțiunile dintre o bancă centrală și multiple ATM-uri ca agenți autonomi.

## Descrierea Problemei

Simularea adresează problema gestionării concurente a tranzacțiilor bancare (retragere, depunere, interogare sold) într-un mediu distribuit.
- **Agentul Bancă (BankAgent)**: Gestionează conturile utilizatorilor, procesează tranzacțiile, asigură consistența datelor și oferă o interfață grafică pentru administrare (creare conturi, monitorizare ATM-uri).
- **Agenții ATM (BaseATMAgent, FullAtmAgent, WithdrawAtmAgent)**: Acționează ca interfețe pentru clienți. Aceștia se înregistrează automat la bancă și permit utilizatorilor să efectueze operațiuni.
  - *FullAtmAgent*: Permite atât retrageri, cât și depuneri.
  - *WithdrawAtmAgent*: Permite doar retrageri.

Sistemul demonstrează utilizarea comunicării între agenți (FIPA-ACL), descoperirea serviciilor (Directory Facilitator - DF) și gestionarea thread-urilor Swing pentru interfețe grafice responsive.

## Structura Proiectului

- `src/`: Codul sursă Java al agenților.
- `lib/`: Biblioteci necesare (jade.jar).
- `bank_data.dat`: Fișier de persistență (generat automat) pentru starea conturilor.
- `initial_data.txt`: (Opțional) Fișier text pentru încărcarea unui set inițial de conturi.

## Instalare și Configurare

### Cerințe
- Java Development Kit (JDK) 8 sau mai recent.
- O instanță VS Code sau Eclipse (opțional).

### Configurare
1. Clonează acest repository.
2. Asigură-te că fișierul `lib/jade.jar` există în proiect.

**Utilizatori IntelliJ / Eclipse**:
- Deschideți proiectul ca **Maven Project** (selectați fișierul `pom.xml`).
- Dependențele vor fi configurate automat.
- Creați o configurație de rulare (Run Configuration) cu Main Class: `jade.Boot` și Program Arguments: `-gui -agents Banca:BankAgent`.

**Configurarea Datelor Inițiale (Opțional)**:
Dacă doriți un set specific de conturi la prima rulare, creați un fișier `initial_data.txt` în rădăcina proiectului cu formatul `user,pin,sold` (fără spații):
```
user1,1234,1000.0
test,0000,500.50
```
Dacă acest fișier nu există și nici `bank_data.dat` nu există, banca va genera automat un cont de test (`user1`).

## Lansare în Execuție

### Din linia de comandă (Windows)
1. Deschideți un terminal în folderul rădăcină al proiectului.
2. Compilați sursele (asigurați-vă că folderul `bin` există):
   ```powershell
   if (!(Test-Path bin)) { mkdir bin }
   javac -cp "lib/jade.jar" -d bin src/*.java
   ```
3. Lansați platforma JADE cu agentul Bancă:
   ```powershell
   java -cp "lib/jade.jar;bin" jade.Boot -gui -agents "Banca:BankAgent"
   ```

### Din linia de comandă (macOS / Linux)
1. Deschideți un terminal în folderul rădăcină al proiectului.
2. Compilați sursele:
   ```bash
   mkdir -p bin
   javac -cp lib/jade.jar -d bin src/*.java
   ```
3. Lansați platforma JADE cu agentul Bancă:
   ```bash
   java -cp lib/jade.jar:bin jade.Boot -gui -agents "Banca:BankAgent"
   ```

### Utilizare
1. **Panoul Băncii**: Se va deschide automat fereastra de administrare a băncii. De aici puteți vedea conturile și puteți lansa ATM-uri noi.
2. **Lansare ATM**: Din tab-ul "ATM-uri" al Băncii, introduceți un nume (ex: "ATM1"), alegeți tipul și apăsați "Lanseaza ATM".
3. **Interacțiune Client**: Se va deschide o fereastră pentru ATM-ul creat. Logați-vă cu un card (ex: "user1", PIN "1234") și efectuați tranzacții.
