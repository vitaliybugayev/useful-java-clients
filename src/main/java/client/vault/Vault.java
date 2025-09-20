package client.vault;

import client.vault.auth_strategy.AuthStrategy;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.testng.TestException;
import utils.CommonUtils;

/**
 * Address and path must be set before using this class
 */
public class Vault {

    private static com.bettercloud.vault.Vault client;
    private static VaultConfig config;
    private static String address;
    private static String path;
    private static AuthStrategy authStrategy;

    public static void setAddress(String address) {
        Vault.address = address;
    }

    public static void setPath(String path) {
        Vault.path = path;
    }

    public static void setAuthStrategy(AuthStrategy authStrategy) {
        Vault.authStrategy = authStrategy;
    }

    private static VaultConfig getConfig() throws VaultException {
        if (address == null || path == null) {
            throw new TestException("Vault address or path isn't set");
        }
        if (config == null) {
            return new VaultConfig()
                    .address(address)
                    .build();
        }
        return config;
    }

    private static void authorize() throws VaultException {
        if (authStrategy == null) {
            throw new TestException("Authentication strategy is not set!");
        }
        var authClientToken = authStrategy.authenticate(client);
        config.token(authClientToken);
    }

    private static com.bettercloud.vault.Vault getClient() throws VaultException {
        if (client == null) {
            config = getConfig();
            client = new com.bettercloud.vault.Vault(config, 2);
            authorize();
        }
        return client;
    }

    public static synchronized String getSecret(String key) {
        try {
            return getClient()
                    .logical()
                    .read(path)
                    .getData().get(key);
        } catch (VaultException e) {
            throw new TestException("Can't get value: " + e);
        }
    }

    public static synchronized <T> T getCredentials(String vaultKey, TypeReference<T> typeReference) {
        var value = getSecret(vaultKey);
        if (value == null || value.isEmpty()) {
            throw new TestException("Can't get credentials by key: " + vaultKey);
        }
        return CommonUtils.unmarshall(value, typeReference);
    }

}
