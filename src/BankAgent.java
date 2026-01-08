import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.wrapper.AgentController;
import jade.wrapper.PlatformController;

public class BankAgent extends Agent {
    private HashMap<String, Account> accounts = new HashMap<>();
    private final String DATA_FILE = "bank_data.dat";
    
    // GUI Components
    private JFrame frame;
    private DefaultTableModel accountsModel;
    private DefaultTableModel atmsModel;
    private JComboBox<String> atmTypeCombo;

    @Override
    protected void setup() {
        System.out.println("Banca " + getLocalName() + " a pornit.");

        // 1. Incarcare date
        loadData();
        
        // 2. GUI Setup
        SwingUtilities.invokeLater(this::createGUI);

        // 3. Inregistrare in DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("banking-service");
        sd.setName("JADE-Bank");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Banca inregistrata in DF.");
        } catch (FIPAException fe) {
        }

        // 4. Comportament mesaje tranzactii
        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    processRequest(msg);
                    updateGUI(); // Refresh UI la fiecare tranzactie
                } else {
                    block();
                }
            }
        });

        // 5. Comportament actualizare lista ATM-uri (Ticker la 5 secunde)
        addBehaviour(new TickerBehaviour(this, 5000) {
            @Override
            protected void onTick() {
                refreshAtmList();
            }
        });
    }

    private void createGUI() {
        frame = new JFrame("Panou Administrare Banca - " + getLocalName());
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Nu inchidem agentul de la X, folosim Delete
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                // Do nothing or minimize
                frame.setState(Frame.ICONIFIED);
            }
        });

        JTabbedPane tabs = new JTabbedPane();

        // --- TAB 1: Gestionare Conturi ---
        JPanel accountsPanel = new JPanel(new BorderLayout());
        
        // Tabel
        String[] accCols = {"Card", "PIN", "Sold"};
        accountsModel = new DefaultTableModel(accCols, 0);
        JTable accTable = new JTable(accountsModel);
        accountsPanel.add(new JScrollPane(accTable), BorderLayout.CENTER);

        // Formular Adaugare
        JPanel accForm = new JPanel(new GridLayout(1, 4));
        JTextField txtCard = new JTextField(); txtCard.setBorder(BorderFactory.createTitledBorder("Card"));
        JTextField txtPin = new JTextField(); txtPin.setBorder(BorderFactory.createTitledBorder("PIN"));
        JTextField txtBal = new JTextField(); txtBal.setBorder(BorderFactory.createTitledBorder("Sold"));
        JButton btnAdd = new JButton("Creare Cont");
        
        btnAdd.addActionListener(e -> {
            String c = txtCard.getText();
            String p = txtPin.getText();
            try {
                double b = Double.parseDouble(txtBal.getText());
                if (!accounts.containsKey(c)) {
                    accounts.put(c, new Account(c, p, b));
                    saveData();
                    updateGUI();
                    txtCard.setText(""); txtPin.setText(""); txtBal.setText("");
                } else {
                    JOptionPane.showMessageDialog(frame, "Cont deja existent!");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Sold invalid!");
            }
        });

        JButton btnDel = new JButton("Sterge Selectat");
        btnDel.addActionListener(e -> {
            int row = accTable.getSelectedRow();
            if (row != -1) {
                String card = (String) accountsModel.getValueAt(row, 0);
                accounts.remove(card);
                saveData();
                updateGUI();
            }
        });

        accForm.add(txtCard); accForm.add(txtPin); accForm.add(txtBal); accForm.add(btnAdd);
        
        JPanel botPanel = new JPanel(new BorderLayout());
        botPanel.add(accForm, BorderLayout.CENTER);
        botPanel.add(btnDel, BorderLayout.EAST);
        accountsPanel.add(botPanel, BorderLayout.SOUTH);

        tabs.addTab("Conturi", accountsPanel);

        // --- TAB 2: Gestionare ATM-uri ---
        JPanel atmsPanel = new JPanel(new BorderLayout());
        
        String[] atmCols = {"Nume Agent", "Tip", "Status"};
        atmsModel = new DefaultTableModel(atmCols, 0);
        JTable atmTable = new JTable(atmsModel);
        atmsPanel.add(new JScrollPane(atmTable), BorderLayout.CENTER);

        JPanel atmControls = new JPanel();
        String[] types = {"FullAtmAgent", "WithdrawAtmAgent"};
        atmTypeCombo = new JComboBox<>(types);
        JTextField txtAtmName = new JTextField(10); txtAtmName.setBorder(BorderFactory.createTitledBorder("Nume ATM"));
        
        JButton btnLaunch = new JButton("Lanseaza ATM");
        btnLaunch.addActionListener(e -> launchATM(txtAtmName.getText(), (String) atmTypeCombo.getSelectedItem()));

        JButton btnKill = new JButton("Opreste ATM Selectat");
        btnKill.addActionListener(e -> {
            int row = atmTable.getSelectedRow();
            if (row != -1) {
                String name = (String) atmsModel.getValueAt(row, 0);
                killATM(name);
            }
        });
        
        JButton btnKillAll = new JButton("Opreste TOATE");
        btnKillAll.setForeground(Color.RED);
        btnKillAll.addActionListener(e -> {
            int rows = atmsModel.getRowCount();
            for (int i = 0; i < rows; i++) {
                String name = (String) atmsModel.getValueAt(i, 0);
                killATM(name);
            }
        });

        JButton btnRefresh = new JButton("Refresh Lista");
        btnRefresh.addActionListener(e -> refreshAtmList());

        atmControls.add(new JLabel("Tip:"));
        atmControls.add(atmTypeCombo);
        atmControls.add(txtAtmName);
        atmControls.add(btnLaunch);
        atmControls.add(btnKill);
        atmControls.add(btnKillAll);
        atmControls.add(btnRefresh);

        atmsPanel.add(atmControls, BorderLayout.SOUTH);

        tabs.addTab("ATM-uri", atmsPanel);

        frame.add(tabs);
        frame.setVisible(true);
        updateGUI();
        refreshAtmList();
    }

    private void updateGUI() {
        SwingUtilities.invokeLater(() -> {
            if (accountsModel == null) return;
            accountsModel.setRowCount(0);
            for (Account a : accounts.values()) {
                accountsModel.addRow(new Object[]{a.cardNumber, a.pin, String.format("%.2f", a.balance)});
            }
        });
    }

    private void refreshAtmList() {
        // Rulam cautarea intr-un thread separat daca suntem pe EDT, 
        // sau direct daca suntem deja in background (TickerBehaviour)
        Runnable searchTask = () -> {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("atm-service");
            template.addServices(sd);
            
            try {
                // Aceasta operatie e blocanta si poate dura
                DFAgentDescription[] result = DFService.search(this, template);
                
                // Actualizarea GUI trebuie facuta doar pe EDT
                SwingUtilities.invokeLater(() -> {
                    if (atmsModel == null) return;
                    atmsModel.setRowCount(0);
                    if (result != null) {
                        for (DFAgentDescription dfd : result) {
                            String localName = dfd.getName().getLocalName();
                            String type = "Unknown";
                            Iterator it = dfd.getAllServices();
                            if (it.hasNext()) {
                                ServiceDescription sDesc = (ServiceDescription) it.next();
                                type = sDesc.getName(); 
                            }
                            atmsModel.addRow(new Object[]{localName, type, "Online"});
                        }
                    }
                });
            } catch (FIPAException e) {
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            new Thread(searchTask).start();
        } else {
            searchTask.run();
        }
    }

    private void launchATM(String name, String className) {
        if (name == null || name.trim().isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Introduceti un nume pentru ATM!");
            return;
        }

        // Check for duplicates first (UI check)
        for (int i = 0; i < atmsModel.getRowCount(); i++) {
             String existingName = (String) atmsModel.getValueAt(i, 0);
             if (existingName.equalsIgnoreCase(name)) {
                 JOptionPane.showMessageDialog(frame, "Eroare: Un ATM cu numele '" + name + "' exista deja!");
                 return;
             }
        }

        // Lansarea unui agent poate dura, o facem in background pentru a nu bloca GUI-ul
        new Thread(() -> {
            try {
                PlatformController container = getContainerController();
                AgentController agent = container.createNewAgent(name, className, new Object[]{});
                agent.start();
                
                // Asteptam putin sa porneasca si sa se inregistreze in DF
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                
                // Refresh lista (va detecta daca e in background si va rula corect)
                refreshAtmList();
                
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(frame, "ATM '" + name + "' lansat cu succes!")
                );
                
            } catch (Exception e) {
                final String errorMsg = e.getMessage();
                SwingUtilities.invokeLater(() -> {
                     if (errorMsg != null && errorMsg.contains("name is already currently used")) {
                         JOptionPane.showMessageDialog(frame, "Eroare: Numele agentului este deja utilizat!");
                     } else {
                         JOptionPane.showMessageDialog(frame, "Eroare la lansare: " + errorMsg);
                     }
                });
            }
        }).start();
    }

    private void killATM(String localName) {
        // Trimitem mesaj de SHUTDOWN la agent
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID(localName, AID.ISLOCALNAME));
            msg.setContent("SHUTDOWN");
            send(msg);
            
            // GUI update rapid (presupunem ca moare)
            // Tickerul va confirma mai tarziu
        } catch (Exception e) {
        }
    }


    private void processRequest(ACLMessage msg) {
        try {
            String content = msg.getContent(); 
            // Format asteptat: "OPERATIE;CARD;PIN;SUMA"
            String[] parts = content.split(";");
            
            if (parts.length < 3) return; // Mesaj invalid
    
            String operation = parts[0];
            String card = parts[1];
            String pin = parts[2];
            
            ACLMessage reply = msg.createReply();
            
            // Verificare existenta cont
            if (!accounts.containsKey(card)) {
                // Putem permite crearea automata pentru demo
                if (operation.equals("CREATE")) {
                    accounts.put(card, new Account(card, pin, 0.0));
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("Cont creat cu succes!");
                    send(reply);
                    saveData(); // Salvam imediat
                    return;
                } else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Cont inexistent!");
                    send(reply);
                    return;
                }
            }
    
            Account acc = accounts.get(card);

            // AUTO-RECOVERY: Sanitizare sold corupt (Infinity/NaN)
            if (Double.isInfinite(acc.balance) || Double.isNaN(acc.balance)) {
                acc.balance = 0.0;
                System.out.println("ALERT: Sold corupt detectat pentru " + card + ". Resetat la 0.0");
                saveData(); // Save the fix
            }
    
            // Verificare PIN
            if (!acc.pin.equals(pin)) {
                reply.setPerformative(ACLMessage.REFUSE);
                reply.setContent("PIN Incorect!");
                send(reply);
                return;
            }
    
            // Procesare Operatiuni
            switch (operation) {
                case "LOGIN" -> {
                    reply.setPerformative(ACLMessage.CONFIRM);
                    reply.setContent(String.format("Login OK. Sold: %.2f", acc.balance));
                }
    
                case "WITHDRAW" -> {
                    try {
                        if (parts.length < 4) throw new NumberFormatException();
                        
                        // Validare format strict (max 2 zecimale)
                        if (!parts[3].matches("^\\d+(\\.\\d{1,2})?$")) {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Format invalid! Folositi max 2 zecimale (ex: 10.50)");
                            break;
                        }

                        double amountW = Double.parseDouble(parts[3]);
                        
                        if (amountW <= 0) {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Suma invalida (trebuie > 0)!");
                        } else if (amountW > 100_000_000) { 
                             reply.setPerformative(ACLMessage.REFUSE);
                             reply.setContent("Suma depaseste limita maxima per tranzactie (100M)!");
                        } else if (acc.balance >= amountW) {
                            acc.balance -= amountW;
                            acc.balance = Math.round(acc.balance * 100.0) / 100.0; // Fix floating point errors
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent(String.format("Retragere reusita! Sold nou: %.2f", acc.balance));
                            saveData(); 
                        } else {
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("Fonduri insuficiente!");
                        }
                    } catch (NumberFormatException nfe) {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Suma invalida! Format numeric asteptat.");
                    }
                }
    
                case "DEPOSIT" -> {
                    try {
                        if (parts.length < 4) throw new NumberFormatException();

                        // Validare format strict (max 2 zecimale)
                        if (!parts[3].matches("^\\d+(\\.\\d{1,2})?$")) {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Format invalid! Folositi max 2 zecimale (ex: 10.50)");
                            break;
                        }

                        double amountD = Double.parseDouble(parts[3]);

                        if (amountD <= 0) {
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("Suma invalida (trebuie > 0)!");
                        } else if (amountD > 100_000_000) { 
                             reply.setPerformative(ACLMessage.REFUSE);
                             reply.setContent("Suma depaseste limita maxima per tranzactie (100M)!");
                        } else {
                            // Check for overflow/infinity
                            if (acc.balance + amountD > 1_000_000_000_000.0 || Double.isInfinite(acc.balance + amountD)) {
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("Sold maxim al contului ar fi depasit!");
                            } else {
                                acc.balance += amountD;
                                acc.balance = Math.round(acc.balance * 100.0) / 100.0; // Fix floating point errors
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent(String.format("Depunere reusita! Sold nou: %.2f", acc.balance));
                                saveData(); 
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        reply.setPerformative(ACLMessage.REFUSE);
                        reply.setContent("Suma invalida! Format numeric asteptat.");
                    }
                }
                    
                case "BALANCE" -> {
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(String.format("Sold curent: %.2f", acc.balance));
                }
            }
            send(reply);
            System.out.println("Banca a raspuns cu: " + reply.getContent());

        } catch (NumberFormatException e) {
        }
    }

    // Metode pentru persistenta (Salvare/Incarcare fisier)
    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(accounts);
            System.out.println("Date salvate in fisier.");
        } catch (IOException e) {
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        File f = new File(DATA_FILE);
        if (f.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                accounts = (HashMap<String, Account>) ois.readObject();
                System.out.println("Date incarcate din fisier: " + accounts.size() + " conturi.");
            } catch (Exception e) {
            }
        } else {
            // Incarcare din fisier text (initial_data.txt) daca nu exista baza de date binara
            File txtFile = new File("initial_data.txt");
            if (txtFile.exists()) {
                try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(txtFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty() || line.startsWith("#")) continue;
                        String[] parts = line.split(",");
                        if (parts.length >= 3) {
                            String u = parts[0].trim();
                            String p = parts[1].trim();
                            try {
                                double s = Double.parseDouble(parts[2].trim());
                                accounts.put(u, new Account(u, p, s));
                            } catch (NumberFormatException nfe) {
                                System.out.println("Sold invalid in txt pentru user: " + u);
                            }
                        }
                    }
                    System.out.println("Date initiale incarcate din initial_data.txt (" + accounts.size() + " conturi).");
                    saveData(); // Salvam in format binar pentru eficienta la urmatoarea rulare
                } catch (Exception e) {
                    System.out.println("Eroare la citirea initial_data.txt: " + e.getMessage());
                }
            }
            
            if (accounts.isEmpty()) {
                accounts.put("user1", new Account("user1", "1234", 1000.0));
                System.out.println("Nu s-au gasit date. Creat cont demo default: user1 / 1234");
            }
        }
    }

    @Override
    protected void takeDown() {
        // Deregistrare din DF la inchidere
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }
        saveData();
        System.out.println("Banca " + getLocalName() + " s-a inchis.");
    }
}
