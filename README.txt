Order Management System - Java JDBC MySQL Application
=======================================================

REQUIREMENTS:
- Java JDK 8 or higher
- MySQL Server running on localhost:3306
- MySQL Connector/J (mysql-connector-java) JAR file

DATABASE SETUP:
1. Start MySQL server
2. Create database: CREATE DATABASE ecommerce_db;
3. The application will create the Orders table automatically

MYSQL CREDENTIALS (hardcoded in application):
- Username: root
- Password: root
- Database: ecommerce_db
- Host: localhost:3306

COMPILATION & EXECUTION:
1. Download MySQL Connector/J from: https://dev.mysql.com/downloads/connector/j/
2. Place mysql-connector-java-x.x.x.jar in the project folder

Compile:
javac -cp ".;mysql-connector-java-*.jar" OrderManagementSystem.java

Run:
java -cp ".;mysql-connector-java-*.jar" OrderManagementSystem

(On Linux/Mac use ':' instead of ';' in classpath)

FEATURES:
a) Insert sample orders (handles duplicate order_id with ON DUPLICATE KEY UPDATE)
b) Display orders by status with quantity > 2, sorted by unit_price DESC
c) Increase order quantity by specified amount
d) Update order status (validates: Pending, Shipped, Delivered)
e) Display total order value (SUM quantity * unit_price) and delete cancelled orders
5) Display all orders (bonus feature)

MENU OPTIONS:
1. Search orders by status (qty > 2)
2. Increase order quantity
3. Update order status
4. Total value & delete cancelled
5. Show all orders
0. Exit