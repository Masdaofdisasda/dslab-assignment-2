package dslab.mailbox;

import dslab.entity.User;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.ResourceBundle;

public class UserStorageSingleton {
    private static volatile UserStorageSingleton INSTANCE;
    private final HashMap<String, User> users;

    private UserStorageSingleton() {
        this.users = new HashMap<>();
    }

    public static synchronized UserStorageSingleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UserStorageSingleton();
        }

        return INSTANCE;
    }

    public boolean doesUserExist(String username) {
        return users.containsKey(username);
    }

    public boolean areCredentialsValid(String username, String pw) {
        User user = users.get(username);
        return user.getPassword().matches(pw);
    }

    public synchronized void parseUsersFromFile(String fileName) {
        ResourceBundle resources = ResourceBundle.getBundle(fileName);
        Enumeration<String> keys = resources.getKeys();

        while (keys.hasMoreElements()) {
            String key = keys.nextElement();

            User user = new User(key, resources.getString(key));

            users.put(key, user);
        }
    }
}
