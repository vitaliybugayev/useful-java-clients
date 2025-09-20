package client.gmail;

public class UsageExample {

    public void example() {
        var gmail = GmailClient.getService();
        System.out.println("Gmail client initialized: " + (gmail != null));
    }
}

