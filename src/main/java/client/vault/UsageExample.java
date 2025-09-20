package client.vault;

import client.vault.auth_strategy.RoleAuthStrategy;
import com.fasterxml.jackson.core.type.TypeReference;

public class UsageExample {

    public record MyCreds(String username, String password) {}

    public void example() {
        Vault.setAddress("http://127.0.0.1:8200");
        Vault.setPath("secret/data/my-app");
        Vault.setAuthStrategy(new RoleAuthStrategy("roleId", "secretId"));

        var secret = Vault.getSecret("someKey");
        var creds = Vault.getCredentials("credsKey", new TypeReference<MyCreds>() {});
        System.out.println("Secret: " + secret);
        System.out.println("Creds username: " + creds.username());
    }
}

