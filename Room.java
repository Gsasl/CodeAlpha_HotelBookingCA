import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

public class Room {
    private int roomNo;
    private String category;
    private double basePrice;

    // Define fixed holidays for the 21% surcharge
    private static final Set<MonthDay> HOLIDAYS = Set.of(
        MonthDay.of(1, 1),   // New Year's Day
        MonthDay.of(2, 14),  // Valentine's Day
        MonthDay.of(5, 1),   // May Day
        MonthDay.of(12, 25), // Christmas
        MonthDay.of(12, 31)  // New Year's Eve
    );

    public Room(int roomNo, String category, double basePrice) {
        this.roomNo = roomNo;
        this.category = category;
        this.basePrice = basePrice;
    }

    public int getRoomNo() { return roomNo; }
    public String getCategory() { return category; }
    public double getBasePrice() { return basePrice; }

    public double getAdjustedPrice(LocalDate date) {
        MonthDay monthDay = MonthDay.from(date);
        
        // 1. Holiday Check (Highest Priority)
        if (HOLIDAYS.contains(monthDay)) {
            return basePrice * 1.21; // 21% Holiday Surcharge
        }
        
        // 2. Weekend Check
        DayOfWeek day = date.getDayOfWeek();
        if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return basePrice * 1.13; // 13% Weekend Surcharge
        } 
        // 3. Weekday Default
        else {
            return basePrice * 0.90; // 10% Weekday Discount
        }
    }

    public String toCSV() {
        return roomNo + "," + category + "," + basePrice;
    }
}