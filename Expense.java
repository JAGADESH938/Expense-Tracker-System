import java.io.Serializable;
import java.time.LocalDate;

/**
 * Represents a single expense with amount, category, date, and an optional
 * note.
 * This is a simple POJO (Plain Old Java Object).
 */
class Expense implements Serializable {
    private double amount;
    private String category;
    private LocalDate date;
    private String note;

    public Expense(double amount, String category, LocalDate date, String note) {
        this.amount = amount;
        this.category = category;
        this.date = date;
        this.note = note;
    }

    // Getters and Setters
    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return String.format("%-10.2f %-15s %-15s %s", amount, category, date.toString(), note);
    }
}