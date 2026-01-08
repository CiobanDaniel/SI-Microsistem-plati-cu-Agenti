import java.io.Serializable;

public class Account implements Serializable {
    public String cardNumber;
    public String pin;
    public double balance;

    public Account(String card, String pin, double bal) {
        this.cardNumber = card;
        this.pin = pin;
        this.balance = bal;
    }
    
    @Override
    public String toString() {
        return "Card: " + cardNumber + " | Sold: " + balance;
    }
}