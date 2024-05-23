package chat_srv;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Base64;

class Client {
    String name;
    PrintWriter pw;

    Client(String name, PrintWriter p) {
        this.name = name;
        this.pw = p;
    }
}

class Clients implements Iterable<Client> {
    private ArrayList<Client> cl;
    private Map<String, String> credentials;

    public Clients() {
        cl = new ArrayList<Client>(10);
        credentials = new HashMap<>();

        credentials.put("alice", "password1");
        credentials.put("bob", "password2");
        credentials.put("ivan", "password3");
    }

    public synchronized void addC(Client c) {
        cl.add(c);
    }

    public synchronized void rmvC(Client c) {
        cl.remove(c);
    }

    public synchronized void sendC(String s) {
        Iterator<Client> itr = cl.iterator();
        while (itr.hasNext()) {
            PrintWriter p = (PrintWriter) (itr.next().pw);
            p.println(s);
        }
    }

    public synchronized int nCl() {
        return cl.size();
    }

    public synchronized boolean authenticate(String username, String password) {
        if (credentials.containsKey(username)) {
            return credentials.get(username).equals(password);
        }
        return false;
    }

    @Override
    public synchronized Iterator<Client> iterator() {
        return cl.iterator();
    }
}


class ServeOneClient extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Client cl;
    private Clients clt;

    public ServeOneClient(Socket s, Clients clt) throws IOException {
        socket = s;
        this.clt = clt;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        // Perform authentication
        String username = in.readLine();
        String password = in.readLine();
        if (!clt.authenticate(username, password)) {
            // Authentication failed
            System.out.println("Authentication failed for client: " + username);
            out.println("ERROR: Authentication failed. Please check your username and password.");
            socket.close(); // Close the socket
            return; // Exit the constructor
        }
        // Authentication successful, add client to the chat
        clt.addC(cl = new Client(username, out));
        start(); // Start the thread to handle client communication
    }

    public void run() {
        System.out.println("join a new client " + cl.name + " - total number " + clt.nCl());
        clt.sendC(" join a new client " + cl.name + ". Total number clients " + clt.nCl()); // informing the clients
                                                                                             // for new participant
        try {
            String str;
            while ((str = in.readLine()) != null) {
                if (str.equals("END"))
                    break;
                System.out.println(str);
                if (str.startsWith("/msg")) {
                    // Extract recipient and message content
                    String[] parts = str.split(" ", 3);
                    String recipient = parts[1];
                    String message = parts[2];
                    sendMessageToUser(recipient, message);
                } else if (str.startsWith("/file")) {
                    // Extract recipient, message content, and file data
                    String[] parts = str.split(" ", 4);
                    if (parts.length == 4) {
                        String recipient = parts[1];
                        String message = parts[2];
                        String fileData = parts[3];
                        sendFileToUser(recipient, message, fileData);
                    } else {
                        System.out.println("Invalid command format for sending file.");
                    }
                } else {
                    clt.sendC(str);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clt.rmvC(cl);
                System.out.println("disconnect client " + cl.name + ". Total number " + clt.nCl());
                clt.sendC("disconnecting client " + cl.name + ". Total number clients connected " + clt.nCl());
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendMessageToUser(String recipient, String message) {
        for (Client recipientClient : clt) {
            if (recipientClient.name.equals(recipient)) {
                recipientClient.pw.println(cl.name + " (private): " + message);
                // Send confirmation message to the sender
                cl.pw.println("You sent a private message to " + recipient + ": " + message);
                break;
            }
        }
    }

    private void sendFileToUser(String recipient, String message, String fileData) {
        try {
            // Decode file data from Base64
            byte[] decodedFileData = Base64.getDecoder().decode(fileData);
            // Example filename, you may need to extract it from the message
            String filename = "received_file.txt";
            // Write file data to disk
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(decodedFileData);
            fos.close();
            // Inform recipient about the received file
            for (Client recipientClient : clt) {
                if (recipientClient.name.equals(recipient)) {
                    recipientClient.pw.println(cl.name + " sent you a file: " + filename);
                     // Send confirmation message to the sender
                     cl.pw.println("You successfully sent a file to " + recipient);
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }
}

public class ChatSrv {
    static final int PORT = 9393; // server port

    public static void main(String[] args) throws IOException {
        ServerSocket s = new ServerSocket(PORT);
        System.out.println("Server Started");
        Clients clt = new Clients();
        try {
            while (true) {
                // Blocks until a connection occurs:
                Socket socket = s.accept();
                try {
                    new ServeOneClient(socket, clt);
                } catch (IOException e) {
                    // If it fails, close the socket,
                    // otherwise, the thread will close it:
                    socket.close();
                }
            }
        } finally {
            s.close();
        }
    }
}