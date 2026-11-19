import java.sql.*;
import java.util.Scanner;

public class OrderManagementSystem {
    private static final String URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String USER = "root", PASS = "root";
    private static Connection conn;
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            initDB();
            insertSamples();
            runMenu();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            closeQuietly(conn);
        }
    }

    private static void initDB() throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Orders (
                    order_id INT PRIMARY KEY, customer_name VARCHAR(100),
                    product_name VARCHAR(100), quantity INT, unit_price DOUBLE, status VARCHAR(30)
                )
            """);
        }
    }

    private static void insertSamples() throws SQLException {
        var sql = upsertSQL();
        Object[][] data = {
            {101, "John Smith", "Laptop", 1, 999.99, "Delivered"},
            {102, "Jane Doe", "Smartphone", 3, 699.99, "Shipped"},
            {103, "Bob Johnson", "Tablet", 2, 349.99, "Pending"},
            {104, "Alice Brown", "Headphones", 5, 149.99, "Cancelled"},
            {105, "Charlie Wilson", "Monitor", 1, 299.99, "Shipped"}
        };
        try (var ps = conn.prepareStatement(sql)) {
            for (var d : data) setOrder(ps, d);
            ps.executeBatch();
        }
    }

    private static void runMenu() throws SQLException {
        while (true) {
            System.out.println("""
                \n=== ORDER MANAGEMENT ===
                1. View by Status (qty>2, price desc)
                2. Update Quantity
                3. Update Status
                4. Total Value & Delete Cancelled
                5. View All
                6. Insert Order
                0. Exit""");
            switch (readInt("Choice: ")) {
                case 1 -> viewByStatus();
                case 2 -> updateQty();
                case 3 -> updateStatus();
                case 4 -> totalAndDelete();
                case 5 -> viewAll();
                case 6 -> insertOrder();
                case 0 -> { System.out.println("Bye!"); return; }
                default -> System.out.println("Invalid");
            }
        }
    }

    private static void viewByStatus() throws SQLException {
        System.out.print("Status: ");
        String status = sc.nextLine().trim();
        printResults("""
            SELECT * FROM Orders WHERE status=? AND quantity>2 ORDER BY unit_price DESC""", status);
    }

    private static void updateQty() throws SQLException {
        int id = readInt("Order ID: "), add = readInt("Add qty: ");
        execute("UPDATE Orders SET quantity=quantity+? WHERE order_id=?", new Object[]{add, id},
            "Quantity updated", "Order not found");
    }

    private static void updateStatus() throws SQLException {
        int id = readInt("Order ID: ");
        System.out.print("New status (Pending/Shipped/Delivered): ");
        String s = sc.nextLine().trim();
        if (!java.util.Set.of("Pending","Shipped","Delivered").contains(s)) {
            System.out.println("Invalid status"); return;
        }
        execute("UPDATE Orders SET status=? WHERE order_id=?", new Object[]{s, id},
            "Status updated", "Order not found");
        if (exists(id)) viewOne(id);
    }

    private static void totalAndDelete() throws SQLException {
        try (var rs = conn.createStatement().executeQuery("SELECT SUM(quantity*unit_price) FROM Orders")) {
            if (rs.next()) System.out.printf("Total Value: $%.2f%n", rs.getDouble(1));
        }
        int id = readInt("Delete ID (if Cancelled): ");
        String status = queryOne("SELECT status FROM Orders WHERE order_id=?", id, String.class);
        if (status == null) System.out.println("Order not found");
        else if ("Cancelled".equalsIgnoreCase(status)) {
            execute("DELETE FROM Orders WHERE order_id=? AND status='Cancelled'", new Object[]{id},
                "Deleted", "Not deleted");
        } else System.out.println("Status is '" + status + "', not Cancelled");
    }

    private static void viewAll() throws SQLException {
        printResults("SELECT * FROM Orders ORDER BY order_id");
    }

    private static void insertOrder() throws SQLException {
        System.out.println("\n--- Insert Order ---");
        Object[] d = {readInt("ID: "), readStr("Customer: "), readStr("Product: "),
            readInt("Qty: "), readDouble("Price: "), readStr("Status: ")};
        execute(upsertSQL(), d, "Inserted/Updated", "Failed");
    }

    // Helpers
    private static String upsertSQL() {
        return "INSERT INTO Orders VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "
            + "customer_name=VALUES(customer_name),product_name=VALUES(product_name),"
            + "quantity=VALUES(quantity),unit_price=VALUES(unit_price),status=VALUES(status)";
    }

    private static void setOrder(PreparedStatement ps, Object[] d) throws SQLException {
        ps.setInt(1, (Integer)d[0]); ps.setString(2, (String)d[1]); ps.setString(3, (String)d[2]);
        ps.setInt(4, (Integer)d[3]); ps.setDouble(5, (Double)d[4]); ps.setString(6, (String)d[5]);
        ps.addBatch();
    }

    private static void printResults(String sql, Object... params) throws SQLException {
        try (var ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i+1, params[i]);
            try (var rs = ps.executeQuery()) {
                boolean any = false;
                System.out.printf("%-10s %-20s %-20s %-8s %-10s %-12s%n", "ID", "Customer", "Product", "Qty", "Price", "Status");
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-10d %-20s %-20s %-8d $%-9.2f %-12s%n",
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getDouble(5), rs.getString(6));
                }
                if (!any) System.out.println("No results");
            }
        }
    }

    private static void execute(String sql, Object[] params, String msgOk, String msgFail) throws SQLException {
        try (var ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i+1, params[i]);
            System.out.println(ps.executeUpdate() > 0 ? msgOk : msgFail);
        }
    }

    private static <T> T queryOne(String sql, int id, Class<T> type) throws SQLException {
        try (var ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                return rs.next() ? type.cast(rs.getObject(1)) : null;
            }
        }
    }

    private static boolean exists(int id) throws SQLException {
        return queryOne("SELECT 1 FROM Orders WHERE order_id=?", id, Integer.class) != null;
    }

    private static void viewOne(int id) throws SQLException {
        printResults("SELECT * FROM Orders WHERE order_id=?", id);
    }

    private static int readInt(String p) {
        while (true) try { System.out.print(p); return Integer.parseInt(sc.nextLine().trim()); }
        catch (Exception e) { System.out.println("Invalid int"); }
    }

    private static double readDouble(String p) {
        while (true) try { System.out.print(p); return Double.parseDouble(sc.nextLine().trim()); }
        catch (Exception e) { System.out.println("Invalid number"); }
    }

    private static String readStr(String p) { System.out.print(p); return sc.nextLine().trim(); }

    private static void closeQuietly(AutoCloseable c) { try { if (c != null) c.close(); } catch (Exception ignored) {} }
}