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
	    long maxTimeout;
	    boolean timeout = false;
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
	    System.out.println("opens connection to "+host);
        Socket clientSocket = new Socket(host, 6789);
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        DataInputStream binFromServer = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        DataOutputStream boutToServer = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
        System.out.println(inFromServer.readLine());
//        boolean finished = false;
        while(true){
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            sentence = inFromUser.readLine();
            outToServer.writeBytes(sentence + '\n');
            cmd = sentence.split("\\s")[0].toUpperCase();

            if (cmd.equals("LIST")){
//                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while(true){
//                    BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    ServerResponse = inFromServer.readLine();
//
                    if(ServerResponse.equals("\0")){
                        break;
                    }else {
                        System.out.println(ServerResponse);
                    }
                }
            }else if(cmd.equals("SEND")){
//                ServerResponse = inFromServer.readLine();
                maxTimeout = new Date().getTime() + 100;
                if (retrSuccess){
                    File f = new File("./"+retrFile);

                    if (byteStreamType.equals("a")){ //Ascii type stream transfer
                        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f,false));
//                        System.out.println(fileSize);
                        for(int i = 0; i < fileSize -1;i++){
//                            if (maxTimeout > new Date().getTime()){
//                                System.out.println("-Transfer taking too long");
//                                timeout = true;
//                                break;
//                            }
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
//                            if (maxTimeout > new Date().getTime()){
//                                System.out.println("-Transfer taking too long");
////                                outToServer.writeBytes();
//                                timeout = true;
//                                break;
//                            }
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
//                    long timeout = new Date().getTime() +
                }
                if (!timeout) {
                    System.out.println(inFromServer.readLine());
                }else{
//                    inFromServer.close();
                    inFromServer.readLine();
                }
            }
            else if(cmd.equals("RETR")){
                ServerResponse = inFromServer.readLine();
//                System.out.println(ServerResponse);
                if (!ServerResponse.equals("-File doesn't exist")){

                    retrFile = sentence.split("\\s")[1];
                    File checkExists = new File("./"+retrFile);
                    if(checkExists.exists()){
                        retrFile = "";

                        retrSuccess = false;
                        System.out.println("-File already exists");
                    }else {
                        timeout = false;
                        retrSuccess = true;
                        fileSize = Integer.parseInt(ServerResponse.split("\0")[0]);
//                        if (ServerResponse.equals("0\0")) {
//                            fileSize = 0;
//                        } else {
//                            fileSize = Integer.parseInt(ServerResponse);
//                        }
                    }
                }
                System.out.println(ServerResponse);

//                System.out.println();
            }
            else if(cmd.equals("DONE")){
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
//                clientSocket.close();
                break;
            }else if(cmd.equals("TYPE")){
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
                if (ServerResponse.equals("+Using Binary mode\0")){
//                    System.out.println("hm bin works brother");
                    byteStreamType = "b";
                }else if(ServerResponse.equals("+Using Ascii mode\0")){
//                    System.out.println("hm Ascii works brother");
                    byteStreamType = "a";
                }else if(ServerResponse.equals("+Using Continuous mode\0")){
//                    System.out.println("hm Continuous works brother");
                    byteStreamType = "c";
                }
            }else if (cmd.equals("STOR")){
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
                if (ServerResponse.charAt(0) == '+'){
                    storFile = sentence.split("\\s")[2];
                    storType = sentence.split("\\s")[1].toUpperCase();
//                    System.out.println(storFile);
//                    System.out.println(storType);
                }
            }
            else if(cmd.equals("SIZE")){
                fileSize = Integer.parseInt(sentence.split("\\s")[1]);
                ServerResponse = inFromServer.readLine();
                System.out.println(ServerResponse);
                if (ServerResponse.equals("+ok, waiting for file\0")){
//                    if ()
                    File sendFile = new File("./"+storFile);
                    System.out.println(sendFile.toString());
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
                                System.out.println("started sending");
                                while ((data = fileSendFile.read()) >= 0){
                                    boutToServer.write(data);
                                }
                                System.out.println("finished sending");
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
//                BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                ServerResponse = inFromServer.readLine();
//            ServerResponse = inFromServer.
//                if ()
                System.out.println(ServerResponse);
            }



        }

        clientSocket.close();

	
    } 
} 
