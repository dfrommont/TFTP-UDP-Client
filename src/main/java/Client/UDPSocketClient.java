package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class UDPSocketClient {

    private static final int MAX_SIZE = 10000; //Max size of file
    protected static DatagramSocket socket = null;

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        System.out.println("UDP Socket Client");
        System.out.println("Please enter IP or name of Server:");
        String name = input.nextLine();
        System.out.println("1. Read file from " + name);
        System.out.println("2. Write file to " + name);
        System.out.println("Pick an option:");
        int flag = Integer.parseInt(input.nextLine()); //User chooses what to do

        try {
            socket = new DatagramSocket();

            socket.setSoTimeout(20000); //Socket has a 20-second timeout

            InetAddress address = InetAddress.getByName(name);
            int port = 1234; //Same port as Server

            if (flag == 1) { //Read from server
                System.out.println("Enter the file name including it's extension:");
                String file_name = input.nextLine();
                try {
                    getFile(socket, address, port, file_name);
                } catch (IOException error) {
                    main(args);
                    throw error;
                }
            }

            if (flag == 2) { //Write to server
                System.out.println("Enter the file name including it's extension (File should be in the 'files' directory):");
                String file_name = input.nextLine();
                try {
                    sendFile(socket, address, port, file_name);
                } catch (Exception e) {
                    if (e instanceof FileNotFoundException) main(args); //If file cannot be found, restart the client
                    throw e;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof SocketTimeoutException) {
                socket.close(); //Close socket if connection has timed out
            }
        } finally {
            if (socket != null) {
                socket.close(); //Close socket at the end of communication
            }
        }
    }

    private static void getFile(DatagramSocket socket, InetAddress address, int port, String file_name) throws Exception {
        String request = "GET " + file_name;
        DatagramPacket sent = new DatagramPacket(request.getBytes(), request.getBytes().length, address, port);
        socket.send(sent); //Send 'GET file name' request to server

        DatagramPacket received = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
        socket.receive(received); //Server returns file

        FileOutputStream fileOutputStream = new FileOutputStream("./files/" + file_name);
        fileOutputStream.write(received.getData(), 0, received.getLength());
        fileOutputStream.close(); //File data is converted into file within '/files'

        String ackMessage = file_name + " received successfully";
        DatagramPacket acknowledgement = new DatagramPacket(ackMessage.getBytes(), ackMessage.getBytes().length, address, port);
        socket.send(acknowledgement); //Client tells server file was received correctly
        System.out.println(file_name + " received successfully from " + acknowledgement.getAddress() + " (Server)");
    }

    private static void sendFile(DatagramSocket socket, InetAddress address, int port, String file_name) throws Exception {
        File file = new File("./files/" + file_name);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileData = new byte[(int) file.length()];
        fileInputStream.read(fileData);
        fileInputStream.close(); //File is converted into an array of bytes

        String request = "PUT " + file_name;
        DatagramPacket sent_name = new DatagramPacket(request.getBytes(), request.getBytes().length, address, port);
        socket.send(sent_name); //Send 'PUT file name' request to server

        DatagramPacket sent_data = new DatagramPacket(fileData, fileData.length, address, port);
        socket.send(sent_data); //Send file data to server

        DatagramPacket received = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
        socket.receive(received); //Receive acknowledgement from the server

        String ackMessage = new String(received.getData(), 0, received.getLength());
        System.out.println(received.getAddress() + " (Server) acknowledgement: " + ackMessage); //Show acknowledgement to client
    }
}