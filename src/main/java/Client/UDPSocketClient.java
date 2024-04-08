package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class UDPSocketClient {

    private static final int MAX_PACKET_SIZE = 10000; // Maximum size of UDP packet
    protected static DatagramSocket clientSocket = null;

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);
        System.out.println("UDP Socket Client");
        System.out.println("Please enter IP or name of Server:");
        String name = input.nextLine();
        System.out.println("1. Read file from " + name);
        System.out.println("2. Write file to " + name);
        System.out.println("Pick an option:");
        int flag = Integer.parseInt(input.nextLine());

        try {
            // Create a UDP socket
            clientSocket = new DatagramSocket();

            // Server address and port
            InetAddress serverAddress = InetAddress.getByName(name);
            int serverPort = 1234;

            if (flag == 1) {
                // Request to retrieve a file by name
                System.out.println("Enter the file name including it's extension:");
                String file_name = input.nextLine();
                try {
                    retrieveFileByName(clientSocket, serverAddress, serverPort, file_name);
                } catch (IOException error) {
                    main(args);
                    throw error;
                }
            }

            if (flag == 2) {
                // Request to send a file to the server
                System.out.println("Enter the file name including it's extension (File should be in the 'files' directory):");
                String file_name = input.nextLine();
                try {
                    sendFileToServer(clientSocket, serverAddress, serverPort, file_name);
                } catch (IOException error) {
                    main(args);
                    throw error;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null) {
                clientSocket.close();
            }
        }
    }

    private static void retrieveFileByName(DatagramSocket socket, InetAddress serverAddress, int serverPort, String fileName) throws IOException {
        // Send request to retrieve file by name
        String request = "GET:" + fileName;
        byte[] requestData = request.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, serverAddress, serverPort);
        socket.send(sendPacket);

        // Receive file data from server
        byte[] receiveData = new byte[MAX_PACKET_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        FileOutputStream fileOutputStream = new FileOutputStream("./files/" + fileName);

        // Write received file data to file
        fileOutputStream.write(receivePacket.getData(), 0, receivePacket.getLength());

        // Close the FileOutputStream
        fileOutputStream.close();

        String ackMessage = fileName + " received successfully";
        byte[] sendData = ackMessage.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
        clientSocket.send(ackPacket);
        System.out.println(fileName + " received successfully from " + ackPacket.getAddress() + " (Server)");
    }

    private static void sendFileToServer(DatagramSocket socket, InetAddress serverAddress, int serverPort, String fileName) throws IOException {
        // Read file
        File file = new File("./files/"+fileName);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileData = new byte[(int) file.length()];
        fileInputStream.read(fileData);
        fileInputStream.close();

        // Send request to send file to server
        String request = "PUT:" + fileName;
        byte[] requestData = request.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(requestData, requestData.length, serverAddress, serverPort);
        socket.send(sendPacket);

        // Send file data to server
        DatagramPacket dataPacket = new DatagramPacket(fileData, fileData.length, serverAddress, serverPort);
        socket.send(dataPacket);

        // Receive acknowledgment from server
        byte[] receiveData = new byte[MAX_PACKET_SIZE];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.receive(receivePacket);

        // Process acknowledgment
        String ackMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
        System.out.println(receivePacket.getAddress() + " (Server) acknowledgement: " + ackMessage);
    }
}