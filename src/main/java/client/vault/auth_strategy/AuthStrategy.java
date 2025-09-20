package client.vault.auth_strategy;

import com.bettercloud.vault.VaultException;

public interface AuthStrategy {
    String authenticate(com.bettercloud.vault.Vault client) throws VaultException;
}
