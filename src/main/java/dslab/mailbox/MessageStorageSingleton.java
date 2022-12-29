package dslab.mailbox;

import dslab.entity.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class MessageStorageSingleton {
    private static volatile MessageStorageSingleton INSTANCE;
    private final HashMap<String, HashMap<String, Message>> messages;

    private MessageStorageSingleton() {
        this.messages = new HashMap<>();
    }

    public static synchronized MessageStorageSingleton getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MessageStorageSingleton();
        }

        return INSTANCE;
    }

    public void storeMessage(String user, Message message) {
        if (!messages.containsKey(user)) messages.put(user, new HashMap<>());

        String id = randomId();

        // clone message so each user has its own in storage
        Message clone = new Message();
        clone.setId(id)
                .setSender(message.getSender())
                .setRecipients(message.getRecipients())
                .setSubject(message.getSubject())
                .setData(message.getData());

        messages.get(user).put(id, clone);

        System.out.println("Message for user " + user + " stored!");
    }

    public List<Message> listMessages(String user) {
        if (!messages.containsKey(user)) return new ArrayList<>();
        return new ArrayList<>(messages.get(user).values());
    }

    public Message getMessage(String user, String id) throws MessageNotFoundException {
        if (!messages.containsKey(user) || !messages.get(user).containsKey(id))
            throw new MessageNotFoundException("error unknown message id");

        return messages.get(user).get(id);
    }

    public void deleteMessage(String user, String id) {
        if (!messages.containsKey(user) || !messages.get(user).containsKey(id))
            throw new MessageNotFoundException("error unknown message id");

        messages.get(user).remove(id);
    }

    private String randomId() {
        StringBuilder id = new StringBuilder();
        Random rand = new Random();
        for (int i = 0; i < 8; i++) {
            id.append((char) ('a' + rand.nextInt(26)));
        }
        return id.toString();
    }
}
