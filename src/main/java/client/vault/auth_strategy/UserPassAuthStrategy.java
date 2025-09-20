package client.vault.auth_strategy;

import com.bettercloud.vault.VaultException;

public class UserPassAuthStrategy implements AuthStrategy {
    private final String username;
    private final String password;

    public UserPassAuthStrategy(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String authenticate(com.bettercloud.vault.Vault client) throws VaultException {
        return client.auth().loginByUserPass(username, password).getAuthClientToken();
    }
}
