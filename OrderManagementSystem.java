import java.sql.*;
import java.util.Scanner;

public class OrderManagementSystem {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ecommerce_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";
    
    private static Connection connection;
    private static Scanner scanner = new Scanner(System.in);
    
    public static void main(String[] args) {
        try {
            connectToDatabase();
            initializeDatabase();
            insertSampleOrders();
            
            boolean exit = false;
            while (!exit) {
                displayMenu();
                int choice = getIntInput("Enter your choice: ");
                
                switch (choice) {
                    case 1:
                        displayOrdersByStatusAndQuantity();
                        break;
                    case 2:
                        increaseOrderQuantity();
                        break;
                    case 3:
                        updateOrderStatus();
                        break;
                    case 4:
                        displayTotalAndDeleteCancelled();
                        break;
                    case 5:
                        displayAllOrders();
                        break;
                    case 6:
                        insertNewOrder();
                        break;
                    case 0:
                        exit = true;
                        System.out.println("Exiting... Thank you!");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
                System.out.println();
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private static void connectToDatabase() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Connected to database successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Please add mysql-connector-java to classpath.");
            throw new SQLException("Driver not found", e);
        }
    }
    
    private static void initializeDatabase() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS Orders (
                order_id INT PRIMARY KEY,
                customer_name VARCHAR(100),
                product_name VARCHAR(100),
                quantity INT,
                unit_price DOUBLE,
                status VARCHAR(30)
            )
        """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Orders table ready.");
        }
    }
    
    private static void insertSampleOrders() throws SQLException {
        String insertSQL = """
            INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                customer_name = VALUES(customer_name),
                product_name = VALUES(product_name),
                quantity = VALUES(quantity),
                unit_price = VALUES(unit_price),
                status = VALUES(status)
        """;
        
        Object[][] sampleOrders = {
            {101, "John Smith", "Laptop", 1, 999.99, "Delivered"},
            {102, "Jane Doe", "Smartphone", 3, 699.99, "Shipped"},
            {103, "Bob Johnson", "Tablet", 2, 349.99, "Pending"},
            {104, "Alice Brown", "Headphones", 5, 149.99, "Cancelled"},
            {105, "Charlie Wilson", "Monitor", 1, 299.99, "Shipped"}
        };
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            for (Object[] order : sampleOrders) {
                pstmt.setInt(1, (Integer) order[0]);
                pstmt.setString(2, (String) order[1]);
                pstmt.setString(3, (String) order[2]);
                pstmt.setInt(4, (Integer) order[3]);
                pstmt.setDouble(5, (Double) order[4]);
                pstmt.setString(6, (String) order[5]);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            System.out.println("Sample orders inserted/updated successfully.");
        }
    }
    
    private static void insertNewOrder() throws SQLException {
        System.out.println("\n--- Insert New Order ---");
        int orderId = getIntInput("Enter Order ID: ");
        System.out.print("Enter Customer Name: ");
        String customerName = scanner.nextLine().trim();
        System.out.print("Enter Product Name: ");
        String productName = scanner.nextLine().trim();
        int quantity = getIntInput("Enter Quantity: ");
        double unitPrice = getDoubleInput("Enter Unit Price: ");
        System.out.print("Enter Status (Pending/Shipped/Delivered/Cancelled): ");
        String status = scanner.nextLine().trim();
        
        String insertSQL = """
            INSERT INTO Orders (order_id, customer_name, product_name, quantity, unit_price, status)
            VALUES (?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                customer_name = VALUES(customer_name),
                product_name = VALUES(product_name),
                quantity = VALUES(quantity),
                unit_price = VALUES(unit_price),
                status = VALUES(status)
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setInt(1, orderId);
            pstmt.setString(2, customerName);
            pstmt.setString(3, productName);
            pstmt.setInt(4, quantity);
            pstmt.setDouble(5, unitPrice);
            pstmt.setString(6, status);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                System.out.println("Order inserted/updated successfully!");
            }
        }
    }
    
    private static double getDoubleInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }
    
    private static void displayMenu() {
        System.out.println("\n=== ORDER MANAGEMENT SYSTEM ===");
        System.out.println("1. View Orders by Status (qty > 2, sorted by price)");
        System.out.println("2. Update Quantity (increase by amount)");
        System.out.println("3. Update Status (Pending/Shipped/Delivered)");
        System.out.println("4. View Total Value & Delete Cancelled Order");
        System.out.println("5. View All Orders");
        System.out.println("6. Insert New Order");
        System.out.println("0. Exit");
    }
    
    private static void displayOrdersByStatusAndQuantity() throws SQLException {
        System.out.print("Enter order status to search: ");
        String status = scanner.nextLine().trim();
        
        String sql = """
            SELECT order_id, customer_name, product_name, quantity, unit_price, status
            FROM Orders
            WHERE status = ? AND quantity > 2
            ORDER BY unit_price DESC
        """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            
            boolean found = false;
            System.out.printf("%-10s %-20s %-20s %-10s %-12s %-15s%n", 
                "Order ID", "Customer", "Product", "Quantity", "Unit Price", "Status");
            System.out.println("-".repeat(90));
            
            while (rs.next()) {
                found = true;
                System.out.printf("%-10d %-20s %-20s %-10d $%-11.2f %-15s%n",
                    rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price"),
                    rs.getString("status"));
            }
            
            if (!found) {
                System.out.println("No orders found with status '" + status + "' and quantity > 2.");
            }
        }
    }
    
    private static void increaseOrderQuantity() throws SQLException {
        int orderId = getIntInput("Enter order ID: ");
        int additionalQuantity = getIntInput("Enter additional quantity: ");
        
        String selectSQL = "SELECT quantity FROM Orders WHERE order_id = ?";
        String updateSQL = "UPDATE Orders SET quantity = quantity + ? WHERE order_id = ?";
        
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {
            selectStmt.setInt(1, orderId);
            ResultSet rs = selectStmt.executeQuery();
            
            if (rs.next()) {
                int currentQuantity = rs.getInt("quantity");
                
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                    updateStmt.setInt(1, additionalQuantity);
                    updateStmt.setInt(2, orderId);
                    int rowsAffected = updateStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        System.out.println("Quantity updated successfully!");
                        System.out.println("Order ID: " + orderId);
                        System.out.println("Previous quantity: " + currentQuantity);
                        System.out.println("New quantity: " + (currentQuantity + additionalQuantity));
                    }
                }
            } else {
                System.out.println("Order with ID " + orderId + " does not exist.");
            }
        }
    }
    
    private static void updateOrderStatus() throws SQLException {
        int orderId = getIntInput("Enter order ID: ");
        System.out.print("Enter new status (Pending/Shipped/Delivered): ");
        String newStatus = scanner.nextLine().trim();
        
        String[] validStatuses = {"Pending", "Shipped", "Delivered"};
        boolean isValidStatus = false;
        for (String valid : validStatuses) {
            if (valid.equalsIgnoreCase(newStatus)) {
                newStatus = valid;
                isValidStatus = true;
                break;
            }
        }
        
        if (!isValidStatus) {
            System.out.println("Invalid status. Valid statuses are: Pending, Shipped, Delivered");
            return;
        }
        
        String selectSQL = "SELECT * FROM Orders WHERE order_id = ?";
        String updateSQL = "UPDATE Orders SET status = ? WHERE order_id = ?";
        
        try (PreparedStatement selectStmt = connection.prepareStatement(selectSQL)) {
            selectStmt.setInt(1, orderId);
            ResultSet rs = selectStmt.executeQuery();
            
            if (rs.next()) {
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSQL)) {
                    updateStmt.setString(1, newStatus);
                    updateStmt.setInt(2, orderId);
                    int rowsAffected = updateStmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        System.out.println("Status updated successfully!");
                        displayOrderDetails(orderId);
                    }
                }
            } else {
                System.out.println("Order with ID " + orderId + " does not exist.");
            }
        }
    }
    
    private static void displayTotalAndDeleteCancelled() throws SQLException {
        String totalSQL = "SELECT SUM(quantity * unit_price) AS total_value FROM Orders";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(totalSQL)) {
            if (rs.next()) {
                double totalValue = rs.getDouble("total_value");
                System.out.printf("Total Order Value: $%.2f%n", totalValue);
            }
        }
        
        int orderId = getIntInput("Enter order ID to delete (only if status is Cancelled): ");
        
        String checkSQL = "SELECT status FROM Orders WHERE order_id = ?";
        String deleteSQL = "DELETE FROM Orders WHERE order_id = ? AND status = 'Cancelled'";
        
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSQL)) {
            checkStmt.setInt(1, orderId);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                String status = rs.getString("status");
                
                if ("Cancelled".equalsIgnoreCase(status)) {
                    try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSQL)) {
                        deleteStmt.setInt(1, orderId);
                        int rowsAffected = deleteStmt.executeUpdate();
                        
                        if (rowsAffected > 0) {
                            System.out.println("Order " + orderId + " deleted successfully (status was Cancelled).");
                        }
                    }
                } else {
                    System.out.println("Cannot delete order " + orderId + ". Status is '" + status + "', not 'Cancelled'.");
                }
            } else {
                System.out.println("Order with ID " + orderId + " does not exist.");
            }
        }
    }
    
    private static void displayAllOrders() throws SQLException {
        String sql = "SELECT * FROM Orders ORDER BY order_id";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            System.out.printf("%-10s %-20s %-20s %-10s %-12s %-15s%n", 
                "Order ID", "Customer", "Product", "Quantity", "Unit Price", "Status");
            System.out.println("-".repeat(90));
            
            while (rs.next()) {
                System.out.printf("%-10d %-20s %-20s %-10d $%-11.2f %-15s%n",
                    rs.getInt("order_id"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("unit_price"),
                    rs.getString("status"));
            }
        }
    }
    
    private static void displayOrderDetails(int orderId) throws SQLException {
        String sql = "SELECT * FROM Orders WHERE order_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                System.out.println("\n--- Updated Order Details ---");
                System.out.println("Order ID: " + rs.getInt("order_id"));
                System.out.println("Customer: " + rs.getString("customer_name"));
                System.out.println("Product: " + rs.getString("product_name"));
                System.out.println("Quantity: " + rs.getInt("quantity"));
                System.out.println("Unit Price: $" + rs.getDouble("unit_price"));
                System.out.println("Status: " + rs.getString("status"));
            }
        }
    }
    
    private static int getIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid integer.");
            }
        }
    }
    
    private static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}