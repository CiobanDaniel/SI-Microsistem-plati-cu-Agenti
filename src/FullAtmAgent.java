import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

public class FullAtmAgent extends BaseAtmAgent {
    @Override
    protected void createGUI() {
        super.createGUI(); // Creeaza partea comuna
        frame.setTitle("ATM Complet (Depunere/Retragere) - " + getLocalName());

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2));
        
        JButton btnLogin = new JButton("Login / Verificare");
        btnLogin.addActionListener(e -> sendTransaction("LOGIN", cardField.getText(), pinField.getText(), "0"));
        
        JButton btnBalance = new JButton("Interogare Sold");
        btnBalance.addActionListener(e -> sendTransaction("BALANCE", cardField.getText(), pinField.getText(), "0"));

        JButton btnWithdraw = new JButton("Retragere Numerar");
        btnWithdraw.addActionListener(e -> sendTransaction("WITHDRAW", cardField.getText(), pinField.getText(), amountField.getText()));

        JButton btnDeposit = new JButton("Depunere Numerar");
        btnDeposit.addActionListener(e -> sendTransaction("DEPOSIT", cardField.getText(), pinField.getText(), amountField.getText()));
        
        JButton btnLogout = new JButton("Delogare");
        btnLogout.addActionListener(e -> {
            cardField.setText("");
            pinField.setText("");
            amountField.setText("");
            displayArea.setText("Va rugam introduceti cardul si PIN-ul.");
        });

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnBalance);
        buttonPanel.add(btnWithdraw);
        buttonPanel.add(btnDeposit);
        buttonPanel.add(btnLogout);

        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}