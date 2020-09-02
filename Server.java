/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*;
import java.nio.file.*;
import java.util.logging.FileHandler;
//import java.nio.file.Files;
//import java.nio.file.NoSuchFileException;
//import java.nio.file.Paths;

class Server {
	public static boolean loggedIn = false;
	public static boolean accountSpecified = false;
	public static boolean passwordSpecified = false;
	public static boolean userSpecified = false;
	public static String username;
	public static String account;
	public static String byteStreamType = "b";
//	public static String directory = "./files";
	public static final String rootDirectory = "./files";
	public static String workingDirectory = "./files";
	public static String oldFileName = "\0";
	public static boolean readyToRetr = false;
	public static String fileToRetr;
	public static String fileToStor = " ";
	public static String storType = " ";
	public static long serverSpace = 20000;
    public static void main(String argv[]) throws Exception 
    {
	String clientCommand;
	String clientResponse;
	String serverResponse = "";
	String[] listData = {};
	ServerSocket welcomeSocket = new ServerSocket(6789);
	System.out.println("listening for connection");
	Socket connectionSocket = welcomeSocket.accept();
	boolean accountSpecified = false;

	DataOutputStream  outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	outToClient.writeBytes("+SK725 SFTP Service" + '\n');
//	System.out.println("its sent lol");
	BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
	DataInputStream binFromClient = new DataInputStream(connectionSocket.getInputStream());
	while(true) {
	    clientResponse = inFromClient.readLine();
	    clientCommand = clientResponse.split("\\s+")[0].toUpperCase();
		if (clientCommand.equals("USER")){
			serverResponse = user(clientResponse.split("\\s+")[1]);
//			outToClient.writeBytes(user(clientResponse.split("\\s+")[1]) + '\n');
		}else if(clientCommand.equals("ACCT")){
			serverResponse = acct(clientResponse.split("\\s+")[1]);
//			outToClient.writeBytes(acct(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("PASS")){
			serverResponse = pass(clientResponse.split("\\s+")[1]);
//			outToClient.writeBytes(pass(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("TYPE") && loggedIn){
			serverResponse = type(clientResponse.split("\\s+")[1]);
//			outToClient.writeBytes(type(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("LIST") && loggedIn){
			if (clientResponse.split("\\s+").length > 2) {
//				System.out.println(list(clientResponse.split("\\s+")[1], directory));
//				outToClient.writeBytes(list(clientResponse.split("\\s+")[1], clientResponse.split("\\s+")[2]));
				serverResponse = list(clientResponse.split("\\s+")[1], clientResponse.split("\\s+")[2]);
			}else{
//				outToClient.writeBytes(list(clientResponse.split("\\s+")[1], directory));
				serverResponse = list(clientResponse.split("\\s+")[1], workingDirectory);
			}
		}else if(clientCommand.equals("CDIR") && loggedIn){
			if (clientResponse.split("\\s+").length> 1) {
				serverResponse = cdir(clientResponse.split("\\s+")[1]);
			}else{
				serverResponse = cdir(rootDirectory);
			}
//			outToClient.writeBytes(cdir(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("KILL") && loggedIn){
			serverResponse = kill(clientResponse.split("\\s+")[1]);
//			outToClient.writeBytes(kill(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("NAME") && loggedIn){
			serverResponse = name(clientResponse.split("\\s+")[1]);
			if (serverResponse.equals("+File exists")){
				oldFileName = clientResponse.split("\\s+")[1];
			}
		}else if(clientCommand.equals("TOBE") && loggedIn){
			if (!oldFileName.isEmpty()) {
				serverResponse = tobe(oldFileName, clientResponse.split("\\s+")[1]);
			}
		}
		else if(clientCommand.equals("DONE")){
			outToClient.writeBytes(done()+ '\n');
			break;
		}else if(clientCommand.equals("RETR") && loggedIn){
			serverResponse = retr(clientResponse.split("\\s+")[1]);
			System.out.println(serverResponse);
//			outToClient.writeBytes(kill(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("SEND") && loggedIn){
			send(fileToRetr,outToClient);
			serverResponse = "file transfer finished";
//			continue;
//			outToClient.writeBytes(kill(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("STOP") && loggedIn){
			serverResponse = stop();
//			outToClient.writeBytes(kill(clientResponse.split("\\s+")[1])+ '\n');
		}else if(clientCommand.equals("STOR") && loggedIn){
			if (clientResponse.split("\\s+").length > 2) {
				serverResponse = stor(clientResponse.split("\\s+")[1], clientResponse.split("\\s+")[2]);
			}else{
				serverResponse = "-Invalid command, not enough arguments";
			}
		}else if (clientCommand.equals("SIZE") && loggedIn ){
			if (!fileToStor.equals(" ")){
				System.out.println(Long.parseLong(clientResponse.split("\\s+")[1]));
				serverResponse = size(Long.parseLong(clientResponse.split("\\s+")[1]));
				outToClient.writeBytes(serverResponse+'\0' +"\r\n");
				if (serverResponse.charAt(0) == '+') {
					serverResponse = saveFile(fileToStor, Long.parseLong(clientResponse.split("\\s+")[1]),inFromClient,binFromClient);
				}
			}else {
				serverResponse = "-File not specified";
			}
		}
		else{
			serverResponse="- Invalid command or not logged in";
		}
		outToClient.writeBytes(serverResponse +'\0' +"\r\n");
	}
    }

    public static String user(String user) throws IOException {
		File file = new File("./identification.txt");
		BufferedReader identification = new BufferedReader(new FileReader(file));
		String fileLine = identification.readLine();
		while ((fileLine = identification.readLine()) != null) {
			if (user.equals("admin")){
				if (loggedIn) {
					return "!" + user + " logged in";
				}
				username = "admin";
				loggedIn = true;
			}
			if (user.equals(fileLine.split(",")[0])){
				if (loggedIn) {
					return "!" + user + " logged in";
				}
				username = user;
				return "+User-id valid, send account and password";
			}
		}
    	return "Invalid user-id, try again";
	}
	public static String acct(String account) throws IOException {
		File file = new File("./identification.txt");
		BufferedReader identification = new BufferedReader(new FileReader(file));
		String fileLine = identification.readLine();
		while ((fileLine = identification.readLine()) != null) {
			if (account.equals(fileLine.split(",")[1]) && username.equals(fileLine.split(",")[0])){
				accountSpecified = true;
				if (loggedIn == true || passwordSpecified){
					passwordSpecified = true;

					return "! Account valid, logged-in";
				}
				else {
					return "+Account valid, send password";
				}
			}
		}
		return "-Invalid account, try again";
	}
	public static String pass(String pass)throws IOException {
		File file = new File("./identification.txt");
		BufferedReader identification = new BufferedReader(new FileReader(file));
		String fileLine = identification.readLine();
		while ((fileLine = identification.readLine()) != null) {
			if (pass.equals(fileLine.split(",")[2]) && username.equals(fileLine.split(",")[0])){
				passwordSpecified = true;
				if (accountSpecified){
					loggedIn = true;
					return "! Logged in";
				}
				else {
					return "+Send account";
				}
			}
		}
		return "Wrong password, try again";
	}

	public static  String type(String t){
    	t = t.toUpperCase();
    	if (t.equals("A")){
    		byteStreamType = "a";
    		return "+Using Ascii mode";
		}else if(t.equals("B")){
			byteStreamType = "b";
			return "+Using Binary mode";
		}else if (t.equals("C")){
			byteStreamType = "c";
			return "+Using Continuous mode";
		}else {
			return "-Type not valid";
		}
	}
	public static String list(String format,String dir) throws IOException {
    	format = format.toUpperCase();
//    	String directory = dir;
    	String name;
    	String lastModified;
    	String owner;
    	String size;
    	StringBuilder data = new StringBuilder();
    	int i = 1;
    	File f;

		f = new File(dir);

		data.append(dir.substring(2) + "\r\n");
		File[] files = f.listFiles();
		for (File file : files){
			name = file.getName();
			if (format.equals("F")){
				System.out.println(file.getName());
				data.append(name+ "\r\n");
			}else if(format.equals("V")){
				lastModified = Files.getLastModifiedTime(Paths.get(file.getPath())).toString();
				owner = Files.getOwner(Paths.get(file.getPath())).toString();
				size = String.valueOf(Files.size(Paths.get(file.getPath())));
				data.append(name + " "  + size + " " + lastModified + " " + owner + "\r\n");
//				System.out.println(data[i]);
			}else{
				return "-invalid option"; //find the proper command for this
			}
		}
//		data.append("\0\r\n");
		return data.toString();
	}

	public static String cdir(String d){
//    	if ((rootDirectory)
		if (d.equals(rootDirectory)){
			workingDirectory = rootDirectory;
			return "!Changed working dir to " + rootDirectory.substring(2);
		}
		File newDirectory = new File(rootDirectory+"/"+d);
		if (newDirectory.isDirectory()){
			workingDirectory = newDirectory.toString();
//			System.out.println(workingDirectory);
			return "!Changed working dir to " +d;
		}else{
			return "-Can't connect to directory because this directory doesnt exist";
		}
	}
	public static String kill(String k) throws IOException {
		try {
			Files.deleteIfExists(Paths.get(workingDirectory+"/"+k));
			return "+"+k+ " deleted";
		} catch (NoSuchFileException e) {
			return "-Not deleted because the file specified doesnt exist";
		}

	}
	public static String name(String f){
    	if (Files.exists(Paths.get(workingDirectory+"/"+f))){
    		return "+File exists";
		}else{return "-Can't find "+f;}
	}
	public static String tobe(String oldName,String newName){
		File oldFile = new File(workingDirectory+"/"+oldName);
		File newFile = new File(workingDirectory+"/"+newName);
		if (newFile.exists()){
			return "-File wasn't renamed because new file name already exists";
		}
		boolean completion = oldFile.renameTo(newFile);
		if(completion){
			return "+"+oldName+ " renamed to "+newName;
		}else{return "-File wasn't renamed";}
	}
	public static String done(){
		return "+SK725 closing connection";
	}
	public static String retr(String file){
		System.out.println("hei threer");
//    	String fileCheck = workingDirectory+"/"+file
		File f = new File(workingDirectory+"/"+file);
		if (!f.exists()){
			return "-File doesn't exist";
		}else{
			readyToRetr = true;
			fileToRetr = workingDirectory+"/"+file;
			return String.valueOf(f.length());
		}
	}
	public static String send(String file,DataOutputStream client) throws IOException {
		if (!readyToRetr){
			return "-File not specified";
		}
		File sendFile = new File(fileToRetr);
		byte[] bArray = new byte[(int)sendFile.length()];
		int data;
		if (byteStreamType.equals("b") || byteStreamType.equals("c")){
			try(FileInputStream outStream = new FileInputStream(sendFile)){
//				int data;
				client.flush();
				while ((data = outStream.read()) >= 0){
					client.write(data);
				}
//				outStream.
				outStream.close();
				client.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			try(BufferedInputStream outStream = new BufferedInputStream(new FileInputStream(sendFile))){
//				int data;
				client.flush();
				data = 0;
				while((data = outStream.read(bArray)) >= 0){
					client.write(bArray,0,data);
				}
				outStream.close();
				client.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	return "";
	}

	public static String stop(){
    	readyToRetr = false;
    	fileToRetr = null;
    	return "+ok, RETR aborted";
	}
	public static String stor(String type,String fileName){
    	File f = new File(workingDirectory+"/"+fileName);
		fileToStor = fileName;
    	if(type.toUpperCase().equals("NEW")){
    		if (f.exists()){
//    			return "+File exists, will create new generation of file";
				fileToStor = " ";
				storType = " ";
    			return "-File exists, but system doesn't support generations";
			}else{
    			storType = "n";
    			return "+File does not exist, will create new file";
			}
		}else if(type.toUpperCase().equals("OLD")){
    		if (f.exists()){
    			storType = "r";
    			return "+Will write over old file";
			}else{
    			storType = "n";
    			return "+Will create new file";
			}

		}else if(type.toUpperCase().equals("APP")){
			if (f.exists()){
				storType = "a";
				return "+Will append to file";
			}else{
				storType = "n";
				return "+Will create file";
			}
		}else{
    		fileToStor = " ";
    		return "-Invalid option";
		}
	}


	public static String size(long size){
		if (size >= serverSpace){
			return "-Not enough room,don't send it";
		}else{
			return "+ok, waiting for file";
		}
	}
	public static String saveFile(String fileToStor,long fileSize,BufferedReader client,DataInputStream bClient) throws IOException {

		if (storType.equals("r")){
			kill(fileToStor);
		}
		File savedFile = new File(workingDirectory+"/"+fileToStor);
    	if (byteStreamType.equals("a")){
    		BufferedOutputStream saveStream = new BufferedOutputStream(new FileOutputStream(savedFile, storType.equals("a")));

    		for (int i = 0; i < (int) fileSize; i++){
    			saveStream.write(client.read());
    		}
    		saveStream.flush();
    		saveStream.close();
    		return "Saved "+fileToStor;

		}else{
			BufferedOutputStream saveStream = new BufferedOutputStream(new FileOutputStream(savedFile, storType.equals("a")));
			int data = 0;
			int i = 0;
			byte[] bArray = new byte[(int) fileSize];
			while (i < (int)fileSize){
				data =bClient.read(bArray);
				saveStream.write(bArray,0,data);
				i+= data;
			}
			saveStream.flush();
			saveStream.close();
			return "Saved "+fileToStor;

		}

    	// "+Saved <file-spec>
		// "-Couldn't save because (reason)
	}
} 

