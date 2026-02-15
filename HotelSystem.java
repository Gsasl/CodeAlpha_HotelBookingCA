import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class HotelSystem {
    private static final String ROOMS_FILE = "rooms.txt";
    private static final String BOOKINGS_FILE = "bookings.txt";
    
    private List<Room> rooms = new ArrayList<>();
    private List<Reservation> bookings = new ArrayList<>();
    private Scanner scanner = new Scanner(System.in);
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d");
    private Random random = new Random();

    public HotelSystem() {
        loadData();
    }

    private void loadData() {
        File roomFile = new File(ROOMS_FILE);
        if (!roomFile.exists()) {
            generate25FloorHotel(); 
            saveData();
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(ROOMS_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    rooms.add(new Room(Integer.parseInt(parts[0]), parts[1], Double.parseDouble(parts[2])));
                }
            } catch (IOException e) { System.out.println("Error loading rooms."); }
        }

        File bookingFile = new File(BOOKINGS_FILE);
        if (bookingFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(BOOKINGS_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(",");
                    bookings.add(new Reservation(parts[0], parts[1], parts[2], parts[3], parts[4], Integer.parseInt(parts[5]), 
                                 LocalDate.parse(parts[6]), LocalDate.parse(parts[7]), Double.parseDouble(parts[8]), 
                                 Double.parseDouble(parts[9]), parts[10]));
                }
            } catch (IOException e) { System.out.println("Error loading bookings."); }
        }
    }

    private void generate25FloorHotel() {
        System.out.println("Initializing 25-Floor Hotel System...");
        for (int f = 1; f <= 10; f++) {
            for (int r = 1; r <= 10; r++) rooms.add(new Room((f * 100) + r, "Standard", 50.0));
        }
        for (int f = 11; f <= 20; f++) {
            for (int r = 1; r <= 10; r++) rooms.add(new Room((f * 100) + r, "Deluxe", 100.0));
        }
        for (int f = 21; f <= 25; f++) {
            for (int r = 1; r <= 10; r++) rooms.add(new Room((f * 100) + r, "Suite", 200.0));
        }
    }

    private void saveData() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(ROOMS_FILE))) {
            for (Room r : rooms) pw.println(r.toCSV());
        } catch (IOException e) { }
        try (PrintWriter pw = new PrintWriter(new FileWriter(BOOKINGS_FILE))) {
            for (Reservation b : bookings) pw.println(b.toCSV());
        } catch (IOException e) { }
    }

    private boolean isRoomAvailable(int roomNo, LocalDate checkIn, LocalDate checkOut) {
        for (Reservation b : bookings) {
            if (b.getRoomNo() == roomNo) {
                if (checkIn.isBefore(b.getCheckOutDate()) && checkOut.isAfter(b.getCheckInDate())) {
                    return false; 
                }
            }
        }
        return true; 
    }

    private String generateHashRef(String name, int roomNo, LocalDate date) {
        String rawData = name + roomNo + date.toString() + System.currentTimeMillis();
        return Integer.toHexString(Math.abs(rawData.hashCode())).toUpperCase();
    }
    
    public void showFares() {
        System.out.println("\n=========================================");
        System.out.println("           HOTEL CA FARE CHART           ");
        System.out.println("=========================================");
        System.out.println("--- Base Rates ---");
        System.out.println("Standard Room : 50.00 USD / night");
        System.out.println("Deluxe Room   : 100.00 USD / night");
        System.out.println("Suite         : 200.00 USD / night");
        System.out.println("\n--- Dynamic Pricing Rules ---");
        System.out.println("Weekdays (Mon-Thu) : 10% Discount applied");
        System.out.println("Weekends (Fri-Sun) : 13% Surcharge applied");
        System.out.println("Holidays           : 21% Surcharge applied");
        System.out.println("\n--- Payment Policies ---");
        System.out.println("Booking Fee (Pay Later) : 11% of Total Fare");
        System.out.println("MFS Transaction Fee     : 1% on BDT converted amount");
        
        // --- NEW PROPERTY POLICIES (Booking.com style) ---
        System.out.println("\n=========================================");
        System.out.println("           PROPERTY POLICIES             ");
        System.out.println("=========================================");
        System.out.println("Check-in    : From 12:30 PM");
        System.out.println("Check-out   : Until 11:30 AM");
        System.out.println("Pets        : Pets are allowed. No extra charges.");
        System.out.println("\nCancellation & Prepayment Policy:");
        System.out.println("Cancellation and prepayment policies vary according to accommodation type.");
        System.out.println("If cancelled up to 1 week (7 days) before date of arrival, a 13% fee of the total");
        System.out.println("fund will be charged. If cancelled later, a 3% charge is applicable for refund processing.");
        System.out.println("=========================================\n");
    }

    public void startBookingFlow() {
        LocalDate checkIn, checkOut;
        try {
            System.out.print("\nEnter Check-In Date (YYYY-MM-DD): ");
            checkIn = LocalDate.parse(scanner.nextLine(), formatter);
            System.out.print("Enter Check-Out Date (YYYY-MM-DD): ");
            checkOut = LocalDate.parse(scanner.nextLine(), formatter);
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            return;
        }

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime cutoffTime = LocalTime.of(12, 1);

        if (checkIn.isBefore(today)) {
            System.out.println("\nError: You cannot book a check-in date in the past.");
            return;
        }

        if (checkIn.equals(today) && now.isAfter(cutoffTime)) {
            System.out.println("\nError: Same-day check-in is not available after 12:01 PM. Please select tomorrow or later.");
            return;
        }

        if (!checkOut.isAfter(checkIn)) {
            System.out.println("\nError: Check-out date must be at least one day after Check-in date.");
            return;
        }

        System.out.println("\nSelect Room Type:");
        System.out.println("1. Standard (Floors 1-10)");
        System.out.println("2. Deluxe   (Floors 11-20)");
        System.out.println("3. Suite    (Floors 21-25)");
        System.out.print("Choice (1-3): ");
        String typeChoice = scanner.nextLine();
        
        String type = "";
        int minFloor = 0, maxFloor = 0;
        
        if (typeChoice.equals("1")) {
            type = "Standard";
            minFloor = 1; maxFloor = 10;
        } else if (typeChoice.equals("2")) {
            type = "Deluxe";
            minFloor = 11; maxFloor = 20;
        } else if (typeChoice.equals("3")) {
            type = "Suite";
            minFloor = 21; maxFloor = 25;
        } else {
            System.out.println("Invalid selection.");
            return;
        }

        int desiredFloor = -1;
        while (true) {
            System.out.print("Enter your desired floor (" + minFloor + "-" + maxFloor + "): ");
            try {
                desiredFloor = Integer.parseInt(scanner.nextLine());
                if (desiredFloor >= minFloor && desiredFloor <= maxFloor) {
                    break; 
                } else {
                    System.out.println("Not a valid floor. Enter floor number " + minFloor + "-" + maxFloor + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }

        List<Room> availableRooms = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getCategory().equalsIgnoreCase(type) && isRoomAvailable(r.getRoomNo(), checkIn, checkOut)) {
                availableRooms.add(r);
            }
        }

        if (availableRooms.isEmpty()) {
            System.out.println("\nSorry, there are no available rooms of this type for the selected dates.");
            return;
        }

        int minDiff = Integer.MAX_VALUE;
        int closestFloor = -1;

        for (Room r : availableRooms) {
            int currentFloor = r.getRoomNo() / 100;
            int diff = Math.abs(currentFloor - desiredFloor);
            if (diff < minDiff) {
                minDiff = diff;
                closestFloor = currentFloor;
            }
        }

        List<Room> closestFloorRooms = new ArrayList<>();
        for (Room r : availableRooms) {
            if (r.getRoomNo() / 100 == closestFloor) {
                closestFloorRooms.add(r);
            }
        }
        
        Room selectedRoom = closestFloorRooms.get(random.nextInt(closestFloorRooms.size()));

        double totalFare = 0;
        LocalDate current = checkIn;
        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);
        
        while (current.isBefore(checkOut)) {
            totalFare += selectedRoom.getAdjustedPrice(current);
            current = current.plusDays(1);
        }

        System.out.println("\n=========================================");
        System.out.println("       PROPOSED BOOKING ITINERARY        ");
        System.out.println("=========================================");
        System.out.println("Room Type  : " + selectedRoom.getCategory());
        
        if (desiredFloor == closestFloor) {
            System.out.println("Room Found : Room " + selectedRoom.getRoomNo() + " (On your desired Floor " + closestFloor + "!)");
        } else {
            System.out.println("Room Found : Room " + selectedRoom.getRoomNo() + " (Floor " + closestFloor + " was the closest available)");
        }
        
        System.out.println("Check-In   : " + checkIn + " (at 12:30 PM)");
        System.out.println("Check-Out  : " + checkOut + " (by 11:30 AM)");
        System.out.println("Duration   : " + totalNights + " Night(s)");
        System.out.printf("Total Fare : %.2f USD\n", totalFare);
        System.out.println("=========================================");

        System.out.print("Does this look good? Press '1' to Proceed, or '2' to Cancel: ");
        String confirmChoice = scanner.nextLine();
        
        if (!confirmChoice.equals("1")) {
            System.out.println("\nBooking cancelled. Returning to main menu...");
            return; 
        }

        System.out.println("\n--- Guest Details ---");
        System.out.print("Full Name     : ");
        String name = scanner.nextLine();
        System.out.print("Contact Number: ");
        String contact = scanner.nextLine();
        System.out.print("Email Address : ");
        String email = scanner.nextLine();

        double bookingFee = totalFare * 0.11;

        System.out.println("\n--- Payment Step 1: Select Amount ---");
        System.out.println("1. Pay Full Amount Now (" + String.format("%.2f", totalFare) + " USD)");
        System.out.println("2. Pay Later (Requires 11% Booking Fee: " + String.format("%.2f", bookingFee) + " USD)");
        System.out.print("Select amount option (1 or 2): ");
        
        String amountChoice = scanner.nextLine();
        double amountToPayUSD = 0;
        String status = "";

        if (amountChoice.equals("1")) {
            amountToPayUSD = totalFare;
            status = "Paid in Full";
        } else if (amountChoice.equals("2")) {
            amountToPayUSD = bookingFee;
            status = "Balance Due";
        } else {
            System.out.println("Payment cancelled.");
            return;
        }

        String bookingRef = generateHashRef(name, selectedRoom.getRoomNo(), checkIn);
        String transactionId = "TXN-" + bookingRef + "-" + (1000 + random.nextInt(9000));

        System.out.println("\n--- Payment Step 2: Select Method ---");
        System.out.println("1. Use Card (USD)");
        
        double dynamicUsdToBdtRate = 119.50 + (random.nextDouble() * 2.50);
        
        System.out.printf("2. Use MFS (Converted to BDT at 1 USD = %.2f BDT + 1%% Transaction Fee)\n", dynamicUsdToBdtRate);
        System.out.print("Choice (1 or 2): ");
        
        String methodChoice = scanner.nextLine();
        
        if (methodChoice.equals("1")) {
            System.out.println("\nRedirecting to Card Payment Processor...");
            simulateDelay(1500); 
            System.out.printf(">>> Payment of %.2f USD confirmed!\n", amountToPayUSD);
        } 
        else if (methodChoice.equals("2")) {
            double amountInBdt = amountToPayUSD * dynamicUsdToBdtRate;
            double mfsFee = amountInBdt * 0.01;
            double totalBdt = amountInBdt + mfsFee;
            
            System.out.printf("\nBase Amount : %.2f BDT\n", amountInBdt);
            System.out.printf("MFS Fee (1%%): %.2f BDT\n", mfsFee);
            System.out.printf("Total to Pay: %.2f BDT\n", totalBdt);
            
            System.out.println("\nRedirecting to MFS Payment Processor...");
            simulateDelay(2000); 
            System.out.printf(">>> Payment of %.2f BDT confirmed!\n", totalBdt);
        } 
        else {
            System.out.println("Invalid payment method. Booking cancelled.");
            return;
        }

        Reservation newBooking = new Reservation(bookingRef, transactionId, name, contact, email, selectedRoom.getRoomNo(), checkIn, checkOut, totalFare, amountToPayUSD, status);
        bookings.add(newBooking);
        saveData();

        generateReceipt(newBooking, selectedRoom, totalNights);
    }

    private void simulateDelay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void viewReservationDetails() {
        System.out.print("\nEnter Reference Number to search: ");
        String searchRef = scanner.nextLine().trim();
        boolean found = false;

        System.out.println("\n--- Reservation details ---");
        for (Reservation b : bookings) {
            if (b.getBookingRef().equalsIgnoreCase(searchRef)) {
                double due = b.getTotalAmount() - b.getAmountPaid();
                System.out.printf("[Name: %s | Chk-In: %s | Chk-Out: %s | Due: %.2f USD | Email: %s]\n",
                        b.getGuestName(), b.getCheckInDate(), b.getCheckOutDate(), due, b.getEmail());
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("No active reservations found for this Reference Number.");
        }
    }

    public void cancelBookingFlow() {
        System.out.print("\nEnter the Booking Reference Number to cancel: ");
        String refNo = scanner.nextLine().trim();

        Iterator<Reservation> iterator = bookings.iterator();
        while (iterator.hasNext()) {
            Reservation b = iterator.next();
            if (b.getBookingRef().equalsIgnoreCase(refNo)) {
                System.out.println("Found booking for " + b.getGuestName() + " (Room " + b.getRoomNo() + ").");
                System.out.print("Are you sure you want to cancel? (Y/N): ");
                String confirm = scanner.nextLine();
                
                if (confirm.equalsIgnoreCase("Y")) {
                    iterator.remove();
                    saveData();
                    System.out.println("Reservation [" + refNo + "] has been successfully cancelled from the system.");
                    
                    // --- NEW FEATURE: Delete the Physical Receipt File ---
                    File receiptFile = new File("Receipt_" + refNo + ".txt");
                    if (receiptFile.exists()) {
                        if(receiptFile.delete()) {
                            System.out.println(">>> Physical receipt file (Receipt_" + refNo + ".txt) has been permanently deleted.");
                        }
                    }
                    return;
                } else {
                    System.out.println("Cancellation aborted.");
                    return;
                }
            }
        }
        System.out.println("Invalid Reference Number. Booking not found.");
    }

    private void generateReceipt(Reservation b, Room r, long nights) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Build the receipt string once
        StringBuilder receipt = new StringBuilder();
        receipt.append("========================================\n");
        receipt.append("          HOTEL CA BOOKING RECEIPT      \n");
        receipt.append("========================================\n");
        receipt.append("Generated   : ").append(timestamp).append("\n");
        receipt.append("Hotline     : Ext. 6621\n");
        receipt.append("----------------------------------------\n");
        receipt.append("Booking Ref : ").append(b.getBookingRef()).append("\n");
        receipt.append("Txn ID      : ").append(b.getTransactionId()).append("\n");
        receipt.append("Guest Name  : ").append(b.getGuestName()).append("\n");
        receipt.append("Contact     : ").append(b.getContact()).append("\n");
        receipt.append("Email       : ").append(b.getEmail()).append("\n");
        receipt.append("Room No     : ").append(b.getRoomNo()).append(" (").append(r.getCategory()).append(")\n");
        receipt.append("Check-In    : ").append(b.getCheckInDate()).append(" (at 12:30 PM)\n");
        receipt.append("Check-Out   : ").append(b.getCheckOutDate()).append(" (by 11:30 AM)\n");
        receipt.append("Duration    : ").append(nights).append(" Night(s)\n");
        receipt.append("----------------------------------------\n");
        receipt.append(String.format("Total Amount: %.2f USD\n", b.getTotalAmount()));
        receipt.append(String.format("Amount Paid : %.2f USD (equivalent)\n", b.getAmountPaid()));
        receipt.append(String.format("Balance Due : %.2f USD\n", (b.getTotalAmount() - b.getAmountPaid())));
        receipt.append("Status      : ").append(b.toCSV().split(",")[10]).append("\n");
        receipt.append("========================================\n");
        receipt.append("      Thank you for choosing Hotel CA!  \n");
        receipt.append("========================================\n");

        // 1. Print to Console
        System.out.println("\n" + receipt.toString());

        // 2. Save to Physical .txt file
        String fileName = "Receipt_" + b.getBookingRef() + ".txt";
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
            pw.print(receipt.toString());
            System.out.println("[ Digital copy saved to your folder as: " + fileName + " ]\n");
        } catch (IOException e) {
            System.out.println("Note: System could not generate the physical text file receipt.");
        }
    }

    public static void main(String[] args) {
        System.out.println("******************************************");
        System.out.println("* Welcome to Hotel_CA_bookingSystem      *");
        System.out.println("******************************************");
        
        HotelSystem hotel = new HotelSystem();

        while (true) {
            System.out.println("\n1. Book a Room");
            System.out.println("2. Show Room Fares & Policies");
            System.out.println("3. View Reservation Details");
            System.out.println("4. Cancel a Booking");
            System.out.println("5. Exit");
            System.out.print("Select an option: ");
            String choice = hotel.scanner.nextLine();

            if (choice.equals("1")) {
                hotel.startBookingFlow();
            } else if (choice.equals("2")) {
                hotel.showFares(); 
            } else if (choice.equals("3")) {
                hotel.viewReservationDetails();
            } else if (choice.equals("4")) {
                hotel.cancelBookingFlow();
            } else if (choice.equals("5")) {
                System.out.println("Goodbye!");
                break;
            } else {
                System.out.println("Invalid option. Please try again.");
            }
        }
    }
}