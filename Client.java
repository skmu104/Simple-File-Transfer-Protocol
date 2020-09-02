/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*;
import java.util.Date;

class Client {
    
    public static void main(String argv[]) throws Exception 
    { 
        String sentence; 
        String ServerResponse;
	    String cmd;
	    String host = "localhost";
	    boolean retrSuccess = false;
	    String retrFile = null;
	    String storFile = null;
	    String storType = null;
	    String byteStreamType = "b";
	    int fileSize = 0;
	    boolean timeout = false;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	    System.out.println("opens connection to "+host);
        Socket clientSocket = new Socket(host, 6789);
        // Defining streams connected to socket to be used later
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        DataInputStream binFromServer = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        DataOutputStream boutToServer = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
        System.out.println(inFromServer.readLine());
        while(true){
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            sentence = inFromUser.readLine();
            outToServer.writeBytes(sentence + '\n');
            cmd = sentence.split("\\s")[0].toUpperCase();
            //Conditional statements for different comamnds to change based on how they are read
            //by the client
              if (cmd.equals("LIST") && sentence.split("\\s").length > 1 ){
                while(true){
                    ServerResponse = inFromServer.readLine();
                    if (ServerResponse.charAt(0) == '-'){
                        System.out.println(ServerResponse);
                        break;
                    }
                    if(ServerResponse.equals("\0")){//null character sent by server indicating end of list
                        break;
                    }else {
                        System.out.println(ServerResponse);
                    }
                }
            }else if(cmd.equals("SEND")){
                if (retrSuccess){
                    File f = new File("./"+retrFile);
                    if (byteStreamType.equals("a")){ //Ascii type stream transfer
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f,false));
                        for(int i = 0; i < fileSize ;i++){
                            stream.write(inFromServer.read());
                        }
                        stream.flush();
                        stream.close();
                        retrSuccess = false;
                        retrFile = null;
                        fileSize = 0;
                    }else{// Binary and Continuous stream transfer
                        FileOutputStream stream = new FileOutputStream(f,false);
                        byte[] bArray = new byte[fileSize];
                        int data;
                        int i = 0;
                        while(i < fileSize){
                             data = binFromServer.read(bArray);
                             stream.write(bArray,0,data);
                             i += data;
                        }
                        stream.flush();
                        stream.close();
                        retrSuccess = false;
                        retrFile = null;
                        fileSize = 0;
                    }
                }
                if (!timeout) {
                    System.out.println(inFromServer.readLine());
                }else{
                    inFromServer.readLine();
                }
            }
            else if(cmd.equals("RETR")){
                ServerResponse = inFromServer.readLine();
                if (!ServerResponse.equals("-File doesn't exist")){
                    retrFile = sentence.split("\\s")[1];
                    File checkExists = new File("./"+retrFile);
                    if(checkExists.exists()){
                        retrFile = "";
                        retrSuccess = false;
                        System.out.println("-File already exists");
                    }else {
                        if (ServerResponse.charAt(0) == '-'){
                            retrSuccess = false;
                        }else{
                            retrSuccess = true;
                            fileSize = Integer.parseInt(ServerResponse.split("\0")[0]);
                        }

                    }
                }
                System.out.println(ServerResponse);
            }
            else if(cmd.equals("DONE")){
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
                clientSocket.close();
                System.exit(0);
            }else if(cmd.equals("TYPE")){
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
                if (ServerResponse.equals("+Using Binary mode\0")){
                    byteStreamType = "b";
                }else if(ServerResponse.equals("+Using Ascii mode\0")){
                    byteStreamType = "a";
                }else if(ServerResponse.equals("+Using Continuous mode\0")){
                    byteStreamType = "c";
                }
            }else if (cmd.equals("STOR")){
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
                if (ServerResponse.charAt(0) == '+'){
                    storFile = sentence.split("\\s")[2];
                    storType = sentence.split("\\s")[1].toUpperCase();
                    File sendFile = new File("./"+storFile);
                    System.out.println(sendFile.length());
                }
            }
            else if(cmd.equals("SIZE")){
                fileSize = Integer.parseInt(sentence.split("\\s")[1]);
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
                if (ServerResponse.equals("+ok, waiting for file\0")){
                    File sendFile = new File("./"+storFile);
//                    System.out.println(sendFile.toString());
                    if (sendFile.exists()){
                        byte[] bArray = new byte[(int) fileSize];
                        try {
                            if (byteStreamType.equals("a")) {
                                BufferedInputStream bufferedSendFile = new BufferedInputStream(new FileInputStream(sendFile));
                                int data = 0;
                                outToServer.flush();
                                while((data = bufferedSendFile.read(bArray)) >= 0){
                                    outToServer.write(bArray,0,data);
                                }
                                bufferedSendFile.close();
                                outToServer.flush();
                            } else {
                                FileInputStream fileSendFile = new FileInputStream(sendFile);
                                int data = 0;
                                while ((data = fileSendFile.read()) >= 0){
                                    boutToServer.write(data);
                                }
                                fileSendFile.close();
                                boutToServer.flush();
                            }
                            System.out.println(inFromServer.readLine());
                        } catch (Exception e){

                        }
                    }else{
                        System.out.println("-Couldn't save because file doesn't exist");
                    }
                }
            }
            else{
                //Default printing of serverResponse
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
            }
        }
    } 
} 
