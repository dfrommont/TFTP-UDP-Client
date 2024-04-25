package Client;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class UDPSocketClient {

    private static final int MAX_SIZE = 512; //Max size of packet that can be communicated

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        System.out.println("UDP Socket Client");
        System.out.println("Please enter IP or name of Server:");
        String serverName = input.nextLine();
        System.out.println("1. Read file from " + serverName);
        System.out.println("2. Write file to " + serverName);
        System.out.println("Pick an option:");
        int option = Integer.parseInt(input.nextLine());

        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(); //Open a new DatagramSocket
            socket.setSoTimeout(30000); //Socket is given a 30-second timeout on communication

            SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(serverName), 69); //Bind socket to machine's IP and port number 69

            if (option == 1) {
                System.out.println("Enter the file name including its extension:");
                String fileName = input.nextLine(); //Retrieve file name from Client
                receiveFile(socket, socketAddress, fileName); //Call method to handle read request
            } else if (option == 2) {
                System.out.println("Enter the file name including its extension (File should be in the 'client_files' directory):");
                String fileName = input.nextLine(); //Retrieve file name from Client
                try {
                    sendFile(socket, socketAddress, fileName); //Call method to handle write request
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Invalid option");
                main(args); //Restart main method if user hasn't chosen one of the two options
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof SocketTimeoutException) {
                System.out.println("Socket has timed out");
                socket.close(); //Close socket if communication has timed out
            }
        } finally {
            if (socket != null) {
                socket.close(); //Close socket after communication has ended if it hasn't already closed
            }
        }
    }

    private static void receiveFile(DatagramSocket socket, SocketAddress socketAddress, String fileName) throws IOException {
        ByteBuffer requestBuffer = ByteBuffer.allocate(4 + fileName.length() + 1 + "octet".length() + 1);
        requestBuffer.putShort((short) 1); //Read opcode
        requestBuffer.put(fileName.getBytes()); //File name
        requestBuffer.put((byte) 0);
        requestBuffer.put("octet".getBytes()); //Octet mode
        requestBuffer.put((byte) 0); //Fill requestBuffer with data in the TFTP format for an RRQ

        byte[] request = requestBuffer.array();
        DatagramPacket requestPacket = new DatagramPacket(request, request.length, socketAddress);
        socket.send(requestPacket); //Package request into a packet and send packet to server

        FileOutputStream fileOutputStream = new FileOutputStream("./files/" + fileName); //Open a file input stream on the desired file
        int blockNumber = 0;

        while (true) {
            DatagramPacket receivedPacket = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
            socket.receive(receivedPacket); //Receive packet back from Server containing file data

            ByteBuffer dataBuffer = ByteBuffer.wrap(receivedPacket.getData());
            short opcode = dataBuffer.getShort();
            if (opcode != 3) {
                break; //Only accept a packet with opcode 3 (DATA opcode)
            }

            int receivedBlockNumber = dataBuffer.getShort();
            if (receivedBlockNumber != blockNumber + 1) {
                break; //Prevent Client from overwriting blocks
            }

            byte[] blockData = Arrays.copyOfRange(receivedPacket.getData(), 4, receivedPacket.getLength());
            fileOutputStream.write(blockData);
            fileOutputStream.flush(); //Write all data to the outputStream and then file

            ByteBuffer ackBuffer = ByteBuffer.allocate(4);
            ackBuffer.putShort((short) 4); // ACK opcode
            ackBuffer.putShort((short) blockNumber);

            byte[] ackData = ackBuffer.array();
            DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, socketAddress);
            socket.send(ackPacket); //Return acknowledgement back to the server for the received data packet with block number

            blockNumber++;

            if (receivedPacket.getLength() < MAX_SIZE) {
                break; //Stop writing file data once all the data has been received
            }
        }

        fileOutputStream.close();
    }

    private static void sendFile(DatagramSocket socket, SocketAddress socketAddress, String fileName) throws Exception {
        ByteBuffer requestBuffer = ByteBuffer.allocate(4 + fileName.length() + 1 + "octet".length() + 1);
        requestBuffer.putShort((short) 2); // WRQ opcode
        requestBuffer.put(fileName.getBytes()); //File name
        requestBuffer.put((byte) 0); // Null byte
        requestBuffer.put("octet".getBytes()); //Octet mode
        requestBuffer.put((byte) 0); //Fill requestBuffer with data in the TFTP format for an WRQ

        byte[] request = requestBuffer.array();
        DatagramPacket requestPacket = new DatagramPacket(request, request.length, socketAddress);
        socket.send(requestPacket); //Package request into Packet then send to Server
        File file = new File("./files/" + fileName); //Open the desired file to be sent
        if (!file.exists()) {
            System.out.println("File not found: " + fileName);
            return; //Stop if the desired file cannot be found
        }

        FileInputStream fileInputStream = new FileInputStream(file); //Open a FileInputStream on the file
        int blockNumber = 1;
        byte[] buffer = new byte[MAX_SIZE];

        while (true) {
            int bytesRead = fileInputStream.read(buffer, 0, MAX_SIZE);
            if (bytesRead == -1) {
                break; //Stop if there is no more data to read
            }

            requestBuffer = ByteBuffer.allocate(bytesRead + 4);
            requestBuffer.putShort((short) 3); //DATA opcode
            requestBuffer.putShort((short) blockNumber);
            requestBuffer.put(buffer, 0, bytesRead); //Package file data into a DATA packet

            request = requestBuffer.array();
            DatagramPacket sentPacket = new DatagramPacket(request, request.length, socketAddress);
            socket.send(sentPacket); //Send DATA packet to Server

            DatagramPacket receivedPacket = new DatagramPacket(new byte[4], 4);
            socket.receive(receivedPacket); //Received packet for packet just sent back from server

            ByteBuffer ackBuffer = ByteBuffer.wrap(receivedPacket.getData());
            short opcode = ackBuffer.getShort();
            if (opcode != 4) {
                return; //Stop writing data if received packet is not an acknowledgement of the DATA
            }
            if (ackBuffer.getShort() != blockNumber) {
                return; //Stop writing  if the block number doesn't match the block number for the current block that was sent
            }

            blockNumber++;
        }

        System.out.println("Write request successful: " + fileName);

        fileInputStream.close();
    }
}