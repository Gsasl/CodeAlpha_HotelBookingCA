import java.time.LocalDate;

public class Reservation {
    private String bookingRef;
    private String transactionId; // NEW FIELD
    private String guestName;
    private String contact;
    private String email;
    private int roomNo;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private double totalAmount;
    private double amountPaid;
    private String status;

    public Reservation(String bookingRef, String transactionId, String guestName, String contact, String email, int roomNo, LocalDate checkInDate, LocalDate checkOutDate, double totalAmount, double amountPaid, String status) {
        this.bookingRef = bookingRef;
        this.transactionId = transactionId;
        this.guestName = guestName;
        this.contact = contact;
        this.email = email;
        this.roomNo = roomNo;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.totalAmount = totalAmount;
        this.amountPaid = amountPaid;
        this.status = status;
    }

    public String getBookingRef() { return bookingRef; }
    public String getTransactionId() { return transactionId; }
    public String getGuestName() { return guestName; }
    public String getContact() { return contact; }
    public String getEmail() { return email; }
    public int getRoomNo() { return roomNo; }
    public LocalDate getCheckInDate() { return checkInDate; }
    public LocalDate getCheckOutDate() { return checkOutDate; }
    public double getTotalAmount() { return totalAmount; }
    public double getAmountPaid() { return amountPaid; }

    public String toCSV() {
        return bookingRef + "," + transactionId + "," + guestName + "," + contact + "," + email + "," + roomNo + "," + 
               checkInDate.toString() + "," + checkOutDate.toString() + "," + totalAmount + "," + amountPaid + "," + status;
    }
}