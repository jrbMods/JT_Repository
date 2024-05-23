package chat_cl;

import java.net.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import javax.swing.*;

public class Client {
    BufferedReader in;
    PrintWriter out;
    Socket socket;
    String name;
    Gui g;

    Client(JFrame f) {
        initAuthentication(); // Prompt user for credentials and connect to server
        g = new Gui(f); // Initialize the graphical user interface
    }

    private void initAuthentication() {
        try {
            // Prompt user for username and password
            String username = JOptionPane.showInputDialog("Please enter your username:");
            String password = JOptionPane.showInputDialog("Please enter your password:");

            // Connect to the server
            socket = new Socket("localhost", 9393);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            // Send username and password to the server for authentication
            out.println(username);
            out.println(password);

            // Wait for server response
            String response = in.readLine();

            // Handle server's response for authentication
            if (response.startsWith("ERROR")) {
                // Display authentication error message
                JOptionPane.showMessageDialog(null, "Authentication failed. Please try again.", "Authentication Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Exit the client on authentication failure
            } else {
                name = username; // Set the client's name upon successful authentication
            }
        } catch (IOException e) {
            // Handle connection or communication error
            JOptionPane.showMessageDialog(null, "Error connecting to server: " + e.getMessage(), "Connection Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // Exit the client on connection error
        }
    }

    class Gui {
        JTextArea serv;
        JTextField cl;

        Gui(JFrame f) {
            f.setLayout(new BorderLayout());
            serv = new JTextArea(20, 10);
            serv.setEditable(false);
            serv.setBackground(new Color(230, 230, 230));
            serv.setFont(new Font("SANS_SERIF", Font.BOLD, 14));
            cl = new JTextField(30);
            f.add("Center", new JScrollPane(serv));
            f.add("South", cl);
            cl.addActionListener(new SrvL());
            (new Rcv()).start();
        }

        class SrvL implements ActionListener {
            public void actionPerformed(ActionEvent e) {
                try {
                    String st = cl.getText();
                    send(st);
                    cl.setText("");
                } catch (Exception ex) {
                    System.out.println("exception: " + ex);
                    System.out.println("closing...");
                    try {
                        socket.close();
                    } catch (Exception expt) {
                        System.out.println(expt);
                    }
                }
            }
        }

        class Rcv extends Thread {
            public void run() {
                for (;;) {
                    try {
                        sleep(400);
                    } catch (InterruptedException e) {
                    }
                    try {
                        serv.append(in.readLine() + "\n");
                        serv.setCaretPosition(serv.getDocument().getLength());
                    } catch (IOException e1) {
                        break;
                    }
                }
                System.out.println("closing reading thread...");
                try {
                    socket.close();
                } catch (Exception expt) {
                    System.out.println(expt);
                }
                System.exit(0);
            }
        }
    }

    void send(String s) {
        if (s.length() == 0) {
            int quit = JOptionPane.showConfirmDialog(null, "Exit chat");
            if (quit == 0) {
                out.println("END");
                System.out.println("closing...");
                try {
                    socket.close();
                } catch (Exception expt) {
                    System.out.println(expt);
                }
                System.exit(0);
            }
        } else
            out.println(name + ": " + s);
    }

    public static void main(String[] args) throws IOException {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        Client cl = new Client(frame);
        frame.setTitle(cl.name + "  (empty line to exit)");
        frame.setSize(500, 300);
        frame.setVisible(true);
    }
}