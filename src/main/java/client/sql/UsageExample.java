package client.sql;

import org.testng.TestException;

import java.sql.Connection;
import java.time.Instant;

public class UsageExample {

    private static final String PUBLISHER_WALLETS_TABLE = "publisher_wallets";

    public static void insertPublisherWallet(Connection connection, String address, String name, byte[] privateKey) {
        var sql = "INSERT INTO %s (address, name, private_key) VALUES (?, ?, ?)".formatted(PUBLISHER_WALLETS_TABLE);
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, address);
            stmt.setString(2, name);
            stmt.setBytes(3, privateKey);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new TestException(e);
        }
    }

    public static PublisherWalletDto getPublisherWallet(Connection connection, String address) {
        var sql = "SELECT * FROM %s WHERE address = ?".formatted(PUBLISHER_WALLETS_TABLE);
        try (var stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, address);
            var rs = stmt.executeQuery();
            if (rs.next()) {
                return new PublisherWalletDto(
                        rs.getString("address"),
                        rs.getString("name"),
                        rs.getBytes("private_key"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                );
            }
        } catch (Exception e) {
            throw new TestException(e);
        }
        return null;
    }

    public record PublisherWalletDto(String address,
                                     String name,
                                     byte[] privateKey,
                                     Instant createdAt,
                                     Instant updatedAt) {}

}
