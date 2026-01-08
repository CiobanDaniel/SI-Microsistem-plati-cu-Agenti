import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

public class BaseAtmAgent extends Agent {
    protected AID bankAID;
    protected JFrame frame;
    protected JTextArea displayArea;
    protected JTextField cardField, pinField, amountField;
    public boolean isLoggedIn = false; // Changed to public for direct access from subclasses

    @Override
    protected void setup() {
        // 1. GUI Setup
        try {
            SwingUtilities.invokeAndWait(() -> createGUI());
        } catch (InterruptedException | InvocationTargetException e) {
        }
        
        // 2. Inregistrare in DF ca ATM
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("atm-service");
        // Use the actual class name (BaseAtmAgent, FullAtmAgent, etc.) as the service name
        sd.setName(this.getClass().getSimpleName()); 
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
        }

        // 3. Comportament Admin (Ascultare comenzi de la Banca/Admin)
        addBehaviour(new jade.core.behaviours.CyclicBehaviour(this) {
            @Override
            public void action() {
                // Filtram doar mesajele de tip REQUEST (comenzi administrative)
                // Astfel nu "furam" raspunsurile (INFORM/REFUSE) asteptate de tranzactii
                jade.lang.acl.MessageTemplate mt = jade.lang.acl.MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = myAgent.receive(mt);
                
                if (msg != null) {
                    if ("SHUTDOWN".equals(msg.getContent())) {
                        logToDisplay(">> Comanda de inchidere primita de la Admin.");
                        doDelete();
                    }
                } else {
                    block();
                }
            }
        });

        // 4. Cautare initiala a bancii
        addBehaviour(new jade.core.behaviours.OneShotBehaviour() {
            @Override
            public void action() {
                findBank();
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
        }
        if (frame != null) {
            // Folosim invokeLater pentru a evita blocarea si InterruptedException la System.exit(0)
            SwingUtilities.invokeLater(() -> {
                try { frame.dispose(); } catch (Exception e) {}
            });
        }
        System.out.println("ATM " + getLocalName() + " s-a inchis.");
    }

    protected void findBank() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("banking-service");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                bankAID = result[0].getName();
                logToDisplay("Conectat la banca: " + bankAID.getLocalName());
            } else {
                logToDisplay("Banca nu a fost gasita!");
            }
        } catch (FIPAException fe) {
        }
    }

    protected void sendTransaction(String operation, String card, String pin, String amount) {
        new Thread(() -> {
            // Validare input locala (Sa nu fie gol)
            if (card.trim().isEmpty() || pin.trim().isEmpty()) {
                logToDisplay(">> Eroare: Introduceti Card si PIN!");
                return;
            }

            if (!operation.equals("LOGIN") && !isLoggedIn) {
                logToDisplay(">> Trebuie sa va AUTENTIFICATI intai (Apasati Login)!");
                return;
            }

            if (bankAID == null) { 
                findBank();
                if(bankAID == null) {
                    logToDisplay("Eroare: Banca indisponibila.");
                    return;
                }
            }
            
            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(bankAID);
            msg.setContent(operation + ";" + card + ";" + pin + ";" + amount);
            send(msg);

            // Asteptare raspuns blocanta (ok aici, suntem pe thread separat)
            ACLMessage reply = blockingReceive(2000); // marim timeout la 2 sec
            
            if (reply != null) {
                String raspuns = reply.getContent();
                logToDisplay("[" + operation + "] " + raspuns);
                if (reply.getPerformative() == ACLMessage.CONFIRM && operation.equals("LOGIN")) {
                    isLoggedIn = true;
                    logToDisplay(">> Autentificare reusita. Sesiune activa.");
                }
            } else {
                logToDisplay("Banca nu raspunde (Timeout).");
            }
        }).start();
    }

    protected void logToDisplay(String text) {
        SwingUtilities.invokeLater(() -> {
            if (displayArea != null) {
                displayArea.append(text + "\n");
                displayArea.setCaretPosition(displayArea.getDocument().getLength());
            } else {
                System.out.println("[GUI Log Error]: " + text);
            }
        });
    }

    // Metoda de suprascris in clasele copil
    protected void createGUI() {
        // Implementarea de baza a ferestrei
        frame = new JFrame("ATM - " + getLocalName());
        frame.setSize(400, 500);
        
        // Cand inchidem fereastra ATM-ului, "omorim" doar acest agent
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                doDelete(); // Aceasta va apela takeDown() care inchide fereastra
            }
        });

        frame.setLayout(new BorderLayout());
        
        // Panou Inputs
        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.add(new JLabel("Card:"));
        cardField = new JTextField("");
        inputPanel.add(cardField);
        
        inputPanel.add(new JLabel("PIN:"));
        pinField = new JTextField("");
        inputPanel.add(pinField);
        
        inputPanel.add(new JLabel("Suma:"));
        amountField = new JTextField("");
        inputPanel.add(amountField);
        
        frame.add(inputPanel, BorderLayout.NORTH);
        
        // Zona afisaj
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        frame.add(new JScrollPane(displayArea), BorderLayout.CENTER);
        
        // Panoul de butoane va fi adaugat de clasele copil
    }
}