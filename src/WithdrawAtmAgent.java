import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;

public class WithdrawAtmAgent extends BaseAtmAgent {
    @Override
    protected void createGUI() {
        super.createGUI();
        frame.setTitle("ATM Retragere - " + getLocalName());

        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton btnLogin = new JButton("Login");
        btnLogin.addActionListener(e -> sendTransaction("LOGIN", cardField.getText(), pinField.getText(), "0"));
        
        JButton btnBalance = new JButton("Sold");
        btnBalance.addActionListener(e -> sendTransaction("BALANCE", cardField.getText(), pinField.getText(), "0"));

        JButton btnWithdraw = new JButton("Retragere");
        btnWithdraw.addActionListener(e -> sendTransaction("WITHDRAW", cardField.getText(), pinField.getText(), amountField.getText()));

        JButton btnLogout = new JButton("Delogare");
        btnLogout.addActionListener(e -> {
            isLoggedIn = false; // Resetare sesiune direct
            cardField.setText("");
            pinField.setText("");
            amountField.setText("");
            displayArea.setText("Va rugam introduceti cardul si PIN-ul.");
        });

        // FARA BUTON DEPOSIT

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnBalance);
        buttonPanel.add(btnWithdraw);
        buttonPanel.add(btnLogout);

        frame.add(buttonPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}