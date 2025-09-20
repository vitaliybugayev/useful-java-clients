package client.vault.auth_strategy;

import com.bettercloud.vault.VaultException;

public class RoleAuthStrategy implements AuthStrategy {
    private final String roleId;
    private final String secretId;

    public RoleAuthStrategy(String roleId, String secretId) {
        this.roleId = roleId;
        this.secretId = secretId;
    }

    @Override
    public String authenticate(com.bettercloud.vault.Vault client) throws VaultException {
        return client.auth().loginByAppRole(roleId, secretId).getAuthClientToken();
    }
}
