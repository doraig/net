package com.pigopoyo;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ezaiuud on 6/9/2017.
 */
public class WebSocketServer {

    public static void main(String args[]) throws IOException, NoSuchAlgorithmException {
            System.out.println(Arrays.toString("Gopi".getBytes()));
        System.out.println(0x30);
        if (args.length != 1 || !args[0].matches("\\d+")) {
            System.err.print("Please provide a port to execute as argument to the program.");
        } else {
            ServerSocket sock = new ServerSocket(Integer.parseInt(args[0]));
            System.out.println("Server started on 127.0.0.1:" + args[0] + ".\nWaiting for connection..");
            startAndAccept(sock);
        }

    }

    private static void startAndAccept(ServerSocket sock) throws IOException, NoSuchAlgorithmException {


        Socket client = sock.accept();

//Create streams
        final InputStream inputStream = client.getInputStream();
        final OutputStream outputStream = client.getOutputStream();
//process stream.

        Scanner scanner = new Scanner(inputStream, "UTF-8");
        String header = scanner.useDelimiter("\\r\\n\\r\\n").next();
        Matcher getMethod = Pattern.compile("^GET").matcher(header);
        if (getMethod.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(header);
            match.find();
            byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + DatatypeConverter
                    .printBase64Binary(
                            MessageDigest
                                    .getInstance("SHA-1")
                                    .digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                            .getBytes("UTF-8")))
                    + "\r\n\r\n")
                    .getBytes("UTF-8");

            outputStream.write(response, 0, response.length);

        } else {

        }
        new Thread(new Runnable() {
            @Override
            public void run() {


                byte[] byteArray = new byte[8645];
                int offset = 0;

                try {
                    while (inputStream.read(byteArray, offset, byteArray.length - offset) != -1) {
                        int length = Math.abs(Math.abs(byteArray[1]) - 128);
                        byte key[] = new byte[4];
                        byte encoded[] = new byte[length];
                        for (int i = 2; i < byteArray.length && byteArray[offset + i] != 0; i++) {
//System.out.println(byteArray[offset+i]);
                            if (key[3] != 0 && encoded.length > (i - 6)) {
                                encoded[i - 6] = byteArray[offset + i];
                            } else if (key[3] == 0) {
                                key[i - 2] = byteArray[offset + i];
                            }
//offset = byteArray.length - offset;
                        }
                        String message = decode(encoded, key);
                        byte[] msg = new StringBuilder().append(message).reverse().toString().getBytes();
                        byte[] len = new byte[1+msg.length]; len[0] = (byte)message.length();
                        System.arraycopy(msg, 0,len, 1,msg.length);
                        byte[] response = new byte[1+len.length]; response[0] = (byte)-127;
                        System.arraycopy(len, 0,response, 1,len.length);

                        outputStream.write(response, 0, response.length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();
      //  byte[] response = {(byte)-127,(byte)4,(byte)71,(byte)111,(byte)112,(byte)105};//new StringBuilder().append(message).reverse().toString().getBytes();
       // outputStream.write("Hi There".getBytes(), 0, "Hi There".getBytes().length);
       /* new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        byte[] response = {(byte)129,(byte)134,(byte)167,(byte)225,(byte)225,(byte)210,(byte)198,(byte)131,(byte)130,(byte)182,(byte)194,(byte)135};//new StringBuilder().append(message).reverse().toString().getBytes();
                        outputStream.write(response, 0, response.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } *//*catch (InterruptedException e) {
                        e.printStackTrace();
                    }*//*
                }
            }
        }).start();
*/

        System.out.println("Connection established to : " + client.getInetAddress().getHostName());
    }

    private void respond(String message, OutputStream outputStream) throws IOException {
        byte[] response = {(byte)129,(byte)134,(byte)167,(byte)225,(byte)225,(byte)210,(byte)198,(byte)131,(byte)130,(byte)182,(byte)194,(byte)135};//new StringBuilder().append(message).reverse().toString().getBytes();
        outputStream.write(response, 0, response.length);
    }

    private static String decode(byte[] encoded, byte[] key) {
        byte[] decode = new byte[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            decode[i] = (byte) (encoded[i] ^ key[i & 0x3]);
        }
        return new String(decode);
    }
}
