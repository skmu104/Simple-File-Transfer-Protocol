/**
 * Code is taken from Computer Networking: A Top-Down Approach Featuring 
 * the Internet, second edition, copyright 1996-2002 J.F Kurose and K.W. Ross, 
 * All Rights Reserved.
 **/

import java.io.*; 
import java.net.*;
import java.nio.file.*;
import java.util.logging.FileHandler;

class Server {
	public static boolean loggedIn = false;
	public static boolean accountSpecified = false;
	public static boolean passwordSpecified = false;
	public static boolean userSpecified = false;
	public static String username;
	public static String byteStreamType = "b";
	public static final String rootDirectory = "./files";
	public static String workingDirectory = "./files";
	public static String oldFileName = "\0";
	public static boolean readyToRetr = false;
	public static String fileToRetr;
	public static String fileToStor = " ";
	public static String storType = " ";
	//Arbritary storage size used.
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
	int arguments = 0;
	// Defining streams connected to socket to be used later
	DataOutputStream  outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	outToClient.writeBytes("+SK725 SFTP Service" + '\n');
	BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
	DataInputStream binFromClient = new DataInputStream(connectionSocket.getInputStream());
	while(true) {
		// reading client commands and arguments
	    clientResponse = inFromClient.readLine();
	    clientCommand = clientResponse.split("\\s+")[0].toUpperCase();
		//used for error handling
	    arguments = clientResponse.split("\\s+").length;
	    //Conditional statements to find command entered
		//Server response is declared by the appropriate command and written out to client at the end
	    if (clientCommand.equals("USER")){
			serverResponse = user(clientResponse.split("\\s+")[1]);
		}else if(clientCommand.equals("ACCT")){
			serverResponse = acct(clientResponse.split("\\s+")[1]);
		}else if(clientCommand.equals("PASS")){
			serverResponse = pass(clientResponse.split("\\s+")[1]);
		}else if(clientCommand.equals("TYPE") && loggedIn && arguments > 1){
			serverResponse = type(clientResponse.split("\\s+")[1]);
		}else if(clientCommand.equals("LIST") && loggedIn && arguments > 1){
			if (clientResponse.split("\\s+").length > 2) {
				serverResponse = list(clientResponse.split("\\s+")[1], clientResponse.split("\\s+")[2]);
			}else{
				// if no directory given, working directory is the default
				serverResponse = list(clientResponse.split("\\s+")[1], workingDirectory);
			}
		}else if(clientCommand.equals("CDIR") && loggedIn){
			if (clientResponse.split("\\s+").length> 1) {
				serverResponse = cdir(clientResponse.split("\\s+")[1]);
			}else{
				serverResponse = cdir(rootDirectory);
			}
		}else if(clientCommand.equals("KILL") && loggedIn && arguments > 1){
			serverResponse = kill(clientResponse.split("\\s+")[1]);
		}else if(clientCommand.equals("NAME") && loggedIn && arguments > 1){
			serverResponse = name(clientResponse.split("\\s+")[1]);
			if (serverResponse.equals("+File exists")){
				oldFileName = clientResponse.split("\\s+")[1];
			}
		}else if(clientCommand.equals("TOBE") && loggedIn && arguments > 1){
			if (!oldFileName.isEmpty()) {
				serverResponse = tobe(oldFileName, clientResponse.split("\\s+")[1]);
			}
		}
		else if(clientCommand.equals("DONE")){
			outToClient.writeBytes(done()+ '\n');
			//If done sent by client, while loop broken and code terminates
			break;
		}else if(clientCommand.equals("RETR") && loggedIn && arguments > 1){
			serverResponse = retr(clientResponse.split("\\s+")[1]);
		}else if(clientCommand.equals("SEND") && loggedIn){
			send(fileToRetr,outToClient);
			serverResponse = "";
		}else if(clientCommand.equals("STOP") && loggedIn){
			serverResponse = stop();
		}else if(clientCommand.equals("STOR") && loggedIn && arguments > 2){
			if (clientResponse.split("\\s+").length > 2) {
				serverResponse = stor(clientResponse.split("\\s+")[1], clientResponse.split("\\s+")[2]);
			}else{
				serverResponse = "-Invalid command, not enough arguments";
			}
		}else if (clientCommand.equals("SIZE") && loggedIn && arguments > 1){
			if (!fileToStor.equals(" ")){
				System.out.println(Long.parseLong(clientResponse.split("\\s+")[1]));
				serverResponse = size(Long.parseLong(clientResponse.split("\\s+")[1]));
				outToClient.writeBytes(serverResponse+'\0' +"\r\n");
				if (serverResponse.charAt(0) == '+') { //Checks if file allowed to be stored
					serverResponse = saveFile(fileToStor, Long.parseLong(clientResponse.split("\\s+")[1]),inFromClient,binFromClient);
				}
			}else {
				serverResponse = "-File not specified";
			}
		}
		else{//Default response if no implemented command is specified
			serverResponse="- Invalid command or not logged in";
		}
		//Outputs out to client
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
		if (dir.equals(workingDirectory)) {
			f = new File(dir);
			data.append(dir.substring(2) + ": \r\n");
		}else{
			f = new File(rootDirectory+'/'+dir);
			data.append(dir + ": \r\n");
		}

		File[] files = f.listFiles();
		for (File file : files){
			name = file.getName();
			if (format.equals("F")){
				data.append(name+ "\r\n");
			}else if(format.equals("V")){
				lastModified = Files.getLastModifiedTime(Paths.get(file.getPath())).toString();
				owner = Files.getOwner(Paths.get(file.getPath())).toString();
				size = String.valueOf(Files.size(Paths.get(file.getPath())));
				data.append(name + " "  + size + " " + lastModified + " " + owner + "\r\n");
			}else{
				return "-invalid option";
			}
		}
		return data.toString();
	}

	public static String cdir(String d){
		if (d.equals(rootDirectory)){
			workingDirectory = rootDirectory;
			return "!Changed working dir to " + rootDirectory.substring(2);
		}
		File newDirectory = new File(rootDirectory+"/"+d);
		if (newDirectory.isDirectory()){
			workingDirectory = newDirectory.toString();
			return "!Changed working dir to " +d;
		}else{
			return "-Can't connect to directory because this directory doesnt exist";
		}
	}
	public static String kill(String k) throws IOException {
		File killFile = new File(workingDirectory+"/"+k);
		if (killFile.exists()){
			killFile.delete();
			return "+"+k+ " deleted";
		}else{
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
				client.flush();
				while ((data = outStream.read()) >= 0){
					client.write(data);
				}
				outStream.close();
				client.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			try(BufferedInputStream outStream = new BufferedInputStream(new FileInputStream(sendFile))){
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
		//Checks if file exists and type of stor and prompts the appropriate change that will be made
    	if(type.toUpperCase().equals("NEW")){
    		if (f.exists()){
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
	}
} 

