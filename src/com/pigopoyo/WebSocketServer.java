package com.pigopoyo;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ezaiuud on 6/9/2017.
 */
public class WebSocketServer {

    private static final String SEC_WEB_SOCKET_KEY = "Sec-WebSocket-Key: (.*)";

    private static final String RESPONSE_HEADER_WEBSOCK = "HTTP/1.1 101 Switching Protocols\r\n"
            + "Connection: Upgrade\r\n"
            + "Upgrade: websocket\r\n"
            + "Sec-WebSocket-Accept: ";
    private static final String END_OF_MSG = "\r\n\r\n";
    private static final String SHA_KEY = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static void main(String args[]) throws IOException, NoSuchAlgorithmException {
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
        String header = scanner.useDelimiter(END_OF_MSG).next(); //Gives the end of the message header.
        Matcher getMethod = Pattern.compile("^GET").matcher(header);
        if (getMethod.find()) {
            Matcher match = Pattern.compile(SEC_WEB_SOCKET_KEY).matcher(header);
            if (match.find()) {
                byte[] response = getBytes(match);
                outputStream.write(response, 0, response.length);
                System.out.println("Connection established to : " + client.getInetAddress().getHostName());
            }
        }
        //Spawn a thread to accept and send out a response from client to the server.
        new Thread(() -> {
            byte[] byteArray = new byte[8645];
            try {
                //TODO: To handel data fragments we need to use the offset value, currently we are just fetching the whole byte (This won't work if the byte exceeds 8645).
                while (inputStream.read(byteArray, 0, byteArray.length) != -1) {
                    int length = Math.abs(Math.abs(byteArray[1]) - 128);
                    byte key[] = new byte[4];
                    byte encoded[] = new byte[length];
                    for (int i = 2; i < byteArray.length && byteArray[i] != 0; i++) {
                        if (key[3] != 0 && encoded.length > (i - 6)) {
                            encoded[i - 6] = byteArray[i];
                        } else if (key[3] == 0) {
                            key[i - 2] = byteArray[i];
                        }
                    }
                    String message = decode(encoded, key);
                    byte[] response = getResponse(message);

                    outputStream.write(response, 0, response.length);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ).start();
        System.out.println("Connection disconnected...");
    }

    private static byte[] getResponse(String message) {
        byte[] msg = new StringBuilder().append(message).reverse().toString().getBytes();
        byte[] len = new byte[1 + msg.length];
        len[0] = (byte) message.length();
        System.arraycopy(msg, 0, len, 1, msg.length);
        byte[] response = new byte[1 + len.length];
        response[0] = (byte) -127;
        System.arraycopy(len, 0, response, 1, len.length);
        return response;
    }

    private static byte[] getBytes(Matcher match) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return (RESPONSE_HEADER_WEBSOCK
                + DatatypeConverter.printBase64Binary(
                        MessageDigest.getInstance("SHA-1").digest(
                                (match.group(1) + SHA_KEY).getBytes("UTF-8")))
                + END_OF_MSG).getBytes("UTF-8");
    }

    private static String decode(byte[] encoded, byte[] key) {
        byte[] decode = new byte[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            decode[i] = (byte) (encoded[i] ^ key[i & 0x3]);
        }
        return new String(decode);
    }
}
