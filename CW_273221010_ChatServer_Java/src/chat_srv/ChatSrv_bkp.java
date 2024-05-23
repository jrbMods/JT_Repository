package chat_srv;

import java.io.*;
import java.net.*;
import java.util.*;

class Client{
    String name;
    PrintWriter pw;
    Client(String name, PrintWriter p){
        this.name = name;
        this.pw =p;
    }
}
class Clients{
    private ArrayList<Client>  cl;
    private Map<String, String> credentials; // Add a map to store usernames and passwords
    public Clients(){
        cl = new ArrayList<Client>(10);
        credentials = new HashMap<>(); // Initialize the map for credentials

        // Hardcoded usernames and passwords
        credentials.put("alice", "password1");
        credentials.put("bob", "password2");
        credentials.put("ivan", "password3");
    }
    public synchronized void addC(Client c){
        cl.add(c);
    }
    public synchronized void rmvC(Client c){
        cl.remove(c);
    }
    public synchronized void sendC(String s){
        Iterator<Client> itr = cl.iterator();
        while(itr.hasNext()) {
            PrintWriter p=(PrintWriter)(itr.next().pw);
            p.println(s);
        }       
    }
    public synchronized int nCl(){
        return cl.size();
    }

    public synchronized boolean authenticate(String username, String password) {
        if (credentials.containsKey(username)) {
            return credentials.get(username).equals(password);
        }
        return false;
    }

}
//...............................................................
class ServeOneClient extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Client cl;
    private Clients clt;
    public ServeOneClient(Socket s,Clients clt)  throws IOException {
        socket = s;
        this.clt =clt;
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
        System.out.println("join a new client "+ cl.name+ " - total number "+ clt.nCl());
        clt.sendC(" join a new client "+ cl.name  + ". Total number clients "+clt.nCl());     //informing the clients for new participant
        try {
            while (true) {
                String str = in.readLine();
                if (str.equals("END")) break;
                System.out.println(str);
                clt.sendC(str);
            }

        } catch (IOException e) {  }
        finally {
            try {
                clt.rmvC(cl);
                System.out.println("disconect client "+cl.name + ". Total number "+clt.nCl());
                clt.sendC("disconecting client "+ cl.name + ". Total number clients connected "+clt.nCl());
                socket.close();
            } catch(IOException e) {}
        }
    }
}
//................................................................................
public class ChatSrv {
    static final int PORT = 9393;      //server port
    public static void main(String[] args) throws IOException {
        ServerSocket s = new ServerSocket(PORT);
        System.out.println("Server Started");
        Clients clt = new Clients();
        try {
            while(true) {
                // Blocks until a connection occurs:
                Socket socket = s.accept();
                try {
                    new ServeOneClient(socket,clt);
                } catch(IOException e) {
                    // If it fails, close the socket,
                    // otherwise the thread will close it:
                    socket.close();
                }
            }
        } finally {
            s.close();
        }
    }
}