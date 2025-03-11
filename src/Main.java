import java.sql.*;

// Short polling and long polling
//  - mimic EC2 server creation,
//  - do quick short polling calls which returns immediately
//  - do long polling calls which returns only once the current status/job changes
public class Main {
    private static String url = System.getenv("DB_URL");
    private static String user = System.getenv("DB_USERNAME");
    private static String password = System.getenv("DB_PASSWORD");
    public static Connection dbConnection;

    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        String ec2ServerId = "15";

        createDBConnection();

        Thread ec2 = new Thread(new EC2Creator(ec2ServerId));
        ec2.start();

        // Do short polling
        String currentStatus = shortPolling(ec2ServerId);
        System.out.println("EC2 status (short polling): " + currentStatus);

        currentStatus = shortPolling(ec2ServerId);
        System.out.println("EC2 status (short polling): " + currentStatus);

        currentStatus = shortPolling(ec2ServerId);
        System.out.println("EC2 status (short polling): " + currentStatus);

        // Do long polling
        currentStatus = longPolling(ec2ServerId, currentStatus);
        System.out.println("EC2 status (long polling): " + currentStatus);
        currentStatus = longPolling(ec2ServerId, currentStatus);
        System.out.println("EC2 status (long polling): " + currentStatus);
    }

    public static String shortPolling(String serverId) throws SQLException, ClassNotFoundException {
        String sql = "SELECT status from servers where id=" + serverId;

        return executeQuery(sql, false);  // return status
    }

    // Returns only when current status changes
    public static String longPolling(String serverId, String clientStatus) throws SQLException, ClassNotFoundException {
        String sql = "SELECT status from servers where id=" + serverId;

        String currentStatus = executeQuery(sql, false);

        while (currentStatus.equals(clientStatus)) {
            currentStatus = executeQuery(sql, false);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return currentStatus;
    }

    public static class EC2Creator extends Thread {
        private String serverId;

        EC2Creator(String serverId) {
            this.serverId = serverId;
        }

        @Override
        public void run() {
            try {
                String sql = "insert into  servers values(" + serverId + ", \"polly_ec2_auto\", NOW(), \"INIT\")";
                executeQuery(sql, true);

                Thread.sleep(5000);

                sql = "update servers set status = \"IN_PROGRESS\" where id=" + this.serverId;
                executeQuery(sql, true);

                Thread.sleep(5000);
                sql = "update servers set status = \"READY\" where id=" + this.serverId;
                executeQuery(sql, true);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String executeQuery(String query, boolean update) throws SQLException, ClassNotFoundException {
        try {
            Statement statement = dbConnection.createStatement();
            ResultSet resultSet;

            if (update)
                statement.executeUpdate(query);
            else {
                resultSet = statement.executeQuery(query);
                if (resultSet.next())
                    return resultSet.getString("status");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "<>";
    }

    public static void createDBConnection() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.jdbc.Driver");

        dbConnection = DriverManager.getConnection(url, user, password);
        dbConnection.setAutoCommit(true);
    }
}