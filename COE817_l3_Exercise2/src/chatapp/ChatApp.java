package chatapp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.StringTokenizer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class ChatApp extends JFrame implements Runnable{
	
	public enum designation{server, client};
	
	private String hostToConnect, alias;
	private int port, myPublicKey, myPrivateKey, mySessionKey;
	private ServerSocket sSocket;
	private Socket mySocket = null;
	private DataOutputStream dataOutStream;
	private DataInputStream dataInStream;
	private BufferedReader  bR;
	private  BufferedWriter bW;
	private SecretKey sharedKey;	
	private byte expectedNounceCreatedByServer, expectedNounceCreatedByClient;
	designation myDesignation;
	Thread cThread;
	
	public static byte[] sent1_signed, sent2_encryptedPublic;
	public static SecretKey sentSecret;
	public static byte[] sentSecretKey;
	
	private JTextField userText;
	private JTextArea chatWindow;
	
	/*
	 * Generates ChatClient object.
	 * 
	 * @hostToConnect - host to connect to. This is used by both server/client
	 * @portToConnect - port on which to open/connect socket
	 * @myAlias - Alias to be used in chat. If undefined, default will be used.
	 * @keyGenerationSeed - Seed used to generate secret key. If null random value will be assigned.
	 * @mydesignation - Designation indicates ChatApp behaviour. Dictates type server or type client
	 */
	public ChatApp (String hostToConnect, int portToConnect, String myAlias, String keyGenerationSeed, designation mydesignation)
	{
		if (hostToConnect == null || Integer.toString(portToConnect) == null || mydesignation == null)
		{
			throw new IllegalArgumentException("Input values are invalid");
		}
		this.hostToConnect = hostToConnect;
		this.port = portToConnect;
		this.alias = (myAlias == null) ? "defaultName" : myAlias;
		this.myDesignation = mydesignation;
		
		//generate public/private keys
		
		//run designation-specific initialization methods
		/*try {
			switch(myDesignation)
			{
				case server:
					
					break;
				case client:
					break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	public void openPort() throws IOException
	{
		sSocket = new ServerSocket(port);
		sSocket.setSoTimeout(10000);		
	}
	
	public void start ()
	{
		System.out.println("Starting " + myDesignation.toString());
		if (cThread == null)
		{
			cThread = new Thread (this, myDesignation.toString()  + "_" + port);
			cThread.start ();
		}
    }
	
	public void setGUI(String windowName)
	{		
		setTitle(windowName);
		setSize(375,300);
		setBackground(Color.gray);
		
		userText = new JTextField();
		userText.setEditable(true);
		userText.addActionListener( 
				new ActionListener(){
					public void actionPerformed(ActionEvent event){
						if (myDesignation == designation.server)
						{
							expectedNounceCreatedByServer = JEncrypDES.generateNounce();
							sendDESEncryptedMessage(event.getActionCommand(), sentSecret, expectedNounceCreatedByServer, expectedNounceCreatedByClient);
						}
						else
						{
							expectedNounceCreatedByClient = JEncrypDES.generateNounce();
							sendDESEncryptedMessage(event.getActionCommand(), sentSecret, expectedNounceCreatedByClient, expectedNounceCreatedByServer);
						}
						
						//sendRawMessage(event.getActionCommand());
						userText.setText(" ");
					}
				});
		add(userText, BorderLayout.SOUTH);
		chatWindow = new JTextArea();
		chatWindow.setEditable(false);
		JScrollPane cWin = new JScrollPane(chatWindow);
		cWin.setPreferredSize(new Dimension(200,200));
		add(cWin, BorderLayout.NORTH);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public void sendRawMessage(String message)
	{
		try {
			/*dataOutStream.writeBytes(message);*/
			bW.write(message);
			bW.newLine();
			bW.flush();
			writeMessageToChatWindow("[ " + ((this.myDesignation == designation.server) ? "server " : "client")  +"]" + message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendRawMessage(int length, byte[] message)
	{
		try {
			//send size of message
			dataOutStream.writeInt(length);
			//send message
			dataOutStream.write(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void sendDESEncryptedMessage(String message, SecretKey sharedKey, byte newNounce, byte expectedNounce)
	{
		try {
			JEncrypDES jDES = new JEncrypDES();
			byte[] byteMessage = jDES.encryptDES(message, newNounce, expectedNounce, sharedKey);

			//send size of message
			dataOutStream.writeInt(byteMessage.length);
			//send message
			dataOutStream.write(byteMessage);

		writeMessageToChatWindow("[ " + ((this.myDesignation == designation.server) ? "server " : "client")  +"]" + message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String decryptDESEncryptedMessage(byte[] message, SecretKey sharedKey, byte expectedNounce)
	{
			JEncrypDES jDes = new JEncrypDES();
			String msg = jDes.decryptDES(message, expectedNounce, sharedKey);
			if ((this.myDesignation == designation.server))
			{
				this.expectedNounceCreatedByClient =  jDes.targetAppExpectedNounce;
			}
			else
			{
				this.expectedNounceCreatedByServer =  jDes.targetAppExpectedNounce;
			}
			
			return msg.trim();
	}
	
	private void writeMessageToChatWindow(String message)
	{
		SwingUtilities.invokeLater(
				new Runnable()
				{
					public void run()
					{
						chatWindow.append("\n" + message);
					}
				}
				);
	}
	
	private void closeApp()
	{
		try {
			/*dataOutStream.close();
			dataInStream.close();	*/
			bR.close();
			bW.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	private void chat()
	{
		/*String message = " You are now connected !";
		sendRawMessage(message);*/
		do{
			try{
				byte[] byteMessage = readByteStream();
				//workaround for shared key transmission
				byte expectedNounce = ((this.myDesignation == designation.server) ? this.expectedNounceCreatedByServer : this.expectedNounceCreatedByClient );
				String receivedMessage = decryptDESEncryptedMessage(byteMessage, this.sentSecret, expectedNounce);
				
				writeMessageToChatWindow("[ " + ((this.myDesignation == designation.server) ? "client" : "server")  +"]" + receivedMessage);
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}while(true);
	}
	
	public void run()
	{	
		//start GUI/UI
		setGUI(this.myDesignation.toString());
		
		try {
			while(true)
			{
				if (this.myDesignation == this.myDesignation.server && (sSocket == null || sSocket.isClosed()) )
				{
					System.out.println("[Server] Socket open on port: " + port + ". Waiting for server");
					openPort();
					
					//wait for connection
					while(mySocket == null)
					{
						mySocket = sSocket.accept();
					}
					System.out.println("[Server] Socket accepted on port " + port + " by remote socket " + mySocket.getRemoteSocketAddress());
				} 
				else
				{
					System.out.println("[Client] Connecting to host:" + this.hostToConnect + " on port:"  + port);
					mySocket =  new Socket(this.hostToConnect, this.port);
					System.out.println("[Client] Connected to host:" + this.hostToConnect + " on port:"  + port);
				}
				
				//setup streams 
				dataOutStream = new DataOutputStream(mySocket.getOutputStream());
				dataInStream = new DataInputStream(mySocket.getInputStream());
				bR = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
				bW = new BufferedWriter(new OutputStreamWriter(mySocket.getOutputStream()));
				
				handshake();
				this.expectedNounceCreatedByServer = (byte) 22;
				this.expectedNounceCreatedByClient = (byte) 22;
				chat();
				
				//Wait for connection/open connection
				//Handshake
					//exchange public keys
					//3 stage handshake
				
				//wait for action and act on action
					//sign message with private key of local app
					//encrypt message with public key of target client
				closeApp();
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private byte[] readByteStream() throws IOException
	{
		byte[] message = null;
		//set public key of opposite machine
		
		int messageLength = dataInStream.readInt();
		if (messageLength > 0)
		{
			message = new byte[messageLength];
			dataInStream.readFully(message, 0, messageLength);
		}
		
		return message;
	}
	
	//Handshake utilizing RSA Public and Private Keys
	void handshake() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, IOException
	{	
		
		//PUBLIC AND PRIVATE KEYS FOR Host A / CLIENT
		BigInteger client_modulus = new BigInteger("4158817544478891990887332699388746060980490741231277290559702504481613466980550442961706478901240858287061074274789302013459087253486188049671010462722451");
		BigInteger client_pubExp = new BigInteger("2783863571710165701756366128212716963200397432188587571344045992006907476723469011955324259588982550734021144915835869507295184475525800489326435352933629");
		BigInteger client_privExp = new BigInteger("2395546180722602066193082622862328107144358038135554220929904800445279801651718781695642149219208087949758635408245613872208809224050269093597648332110397");

	        
		// PUBLIC AND PRIVATE KEYS FOR Host B / SERVER
	    BigInteger server_modulus = new BigInteger("4599262798794308708014101451216442903941121046201886206956480723533329439440024060097691229615851392513071323777864307630956530138394825517668034826098437");
	    BigInteger server_pubExp = new BigInteger("2878420510383165733314211612508276097319619487646498929668095229783127291389903837072535693852639094406307595634344542615532828664237932111912378158009611");
	    BigInteger server_privExp = new BigInteger("413607170569240106355162130796497957404600590139690077874292496075867173940387286464334397558879911527577289794644345851732962834469537529656069768837211");
		
	    // Public keys of opposing machine is already known
	    BigInteger pubExp ;
	    BigInteger pubMod;
	    
		JEncrypDES jDes = new JEncrypDES();
		JEncryptRSA jRSA = new JEncryptRSA();
		int n1, n2, n1_temp, n2_temp;
		String peerID = null, message = null;
		byte[] byteMessage = null;
		StringTokenizer sTkn;
		
		if (myDesignation == designation.server)
		{
			//set public key of opposite machine
			pubExp = client_pubExp;
			pubMod = client_modulus;
			
			//RECV E(PUb, [N1||IDa])
			this.writeMessageToChatWindow("Handshake PHASE 1");
			byteMessage = readByteStream();
			String decryptedMessage = JEncryptRSA.decryptRSA(byteMessage, server_privExp, server_modulus);
			sTkn = new StringTokenizer(decryptedMessage,"||");
			expectedNounceCreatedByClient = Byte.parseByte(sTkn.nextToken().trim());
			peerID = sTkn.nextToken().trim();
			writeMessageToChatWindow("[client]: " + decryptedMessage);
			
			//SEND E(PUa,[N1||N2])
			this.writeMessageToChatWindow("Handshake PHASE 2");
			expectedNounceCreatedByServer = JEncrypDES.generateNounce();
			message = "" +  expectedNounceCreatedByClient  + "||" + expectedNounceCreatedByServer;
			this.writeMessageToChatWindow("Sending: " + message);
			byteMessage = jRSA.encryptRSA(message, pubExp, pubMod);
			this.sendRawMessage(byteMessage.length,byteMessage);
			
			//RECV E(PUb,[N2]])
			this.writeMessageToChatWindow("Handshake PHASE 3");
			byteMessage = readByteStream();
			decryptedMessage = JEncryptRSA.decryptRSA(byteMessage, server_privExp, server_modulus);
			sTkn = new StringTokenizer(decryptedMessage,"||");
			n2_temp = Integer.parseInt(sTkn.nextToken().trim());
			
			if (n2_temp != expectedNounceCreatedByServer || sTkn.hasMoreElements())
			{
				//Illegal nounce value / too many elements. Drop handshake
				writeMessageToChatWindow("[server]: ILLEGAL NOUNCE VALUE FOUND. TERMINATING HANDSHAKE");
				return;
			}
			writeMessageToChatWindow("[client]: " + decryptedMessage);
			
			//RECV E(PUb,E(PRa, Ks))
			this.writeMessageToChatWindow("Handshake PHASE 4");
			byteMessage = readByteStream();
			/*	
			decryptedMessage = JEncryptRSA.decryptRSA(byteMessage, server_privExp, server_modulus);
			decryptedMessage= JEncryptRSA.decryptRSA(decryptedMessage.getBytes(), client_pubExp, client_modulus);
			byte[] decodedKey = Base64.getDecoder().decode(decryptedMessage);
			this.sharedKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "DES");
			writeMessageToChatWindow("[client]: shared key " + sharedKey.toString());*/
			
			if (Arrays.equals(byteMessage, sent2_encryptedPublic))
				System.out.println("sent2 they match. Message was delivered correctly.");
			
			decryptedMessage = JEncryptRSA.decryptRSA(byteMessage, server_privExp, server_modulus);
			byte[] tempMsg = decryptedMessage.getBytes();
			if (Arrays.equals(decryptedMessage.getBytes(), sent1_signed))
				System.out.println("sent1 they match");
			else
				System.out.println("sent1 they do not match");
			//decryptedMessage = JEncryptRSA.decryptRSA(byteMessage, client_pubExp, client_modulus);
			decryptedMessage = JEncryptRSA.decryptRSA(decryptedMessage.getBytes(), server_privExp, server_modulus);
			if (Arrays.equals(decryptedMessage.getBytes(), sentSecretKey))
				System.out.println("key they match");
			else
				System.out.println("key they do not match");
			System.out.println("r2 decr with client public enc str " + byteMessage.toString().trim());
		}
		else
		{
			//set public key of opposite machine
			pubExp = server_pubExp;
			pubMod = server_modulus;
			
			
			//SEND E(PUb, [N1||IDa])
			this.writeMessageToChatWindow("Handshake PHASE 1");
			expectedNounceCreatedByClient = JEncrypDES.generateNounce();
			message = expectedNounceCreatedByClient + "||" + this.alias;
			this.writeMessageToChatWindow("Sending: " + message);
			byteMessage = JEncryptRSA.encryptRSA(message, server_pubExp, server_modulus);
			this.sendRawMessage(byteMessage.length,byteMessage);
			
			//RECEIVE E(PUa,[N1||N2])
			this.writeMessageToChatWindow("Handshake PHASE 2");
			byteMessage = readByteStream();
			String decryptedMessage = JEncryptRSA.decryptRSA(byteMessage, client_privExp, client_modulus);
			sTkn = new StringTokenizer(decryptedMessage,"||");
			n1_temp = Integer.parseInt(sTkn.nextToken().trim());
			
			if ((byte)n1_temp != expectedNounceCreatedByClient)
			{
				//Illegal nounce value. Drop handshake
				writeMessageToChatWindow("[client]: ILLEGAL NOUNCE VALUE FOUND. TERMINATING HANDSHAKE");
				return;
			}
			
			expectedNounceCreatedByServer = Byte.parseByte(sTkn.nextToken());
			writeMessageToChatWindow("[server]: " + decryptedMessage);
			
			//SEND E(PUb,[N2]])
			this.writeMessageToChatWindow("Handshake PHASE 3");
			message = "" + expectedNounceCreatedByServer;
			this.writeMessageToChatWindow("Sending: " + message);
			byteMessage = JEncryptRSA.encryptRSA(message, server_pubExp, server_modulus);
			this.sendRawMessage(byteMessage.length,byteMessage);
			
			//SEND E(PUb,E(PRa, Ks))
			this.writeMessageToChatWindow("Handshake PHASE 4");
			//generate shared key
			sharedKey = jDes.desKeyGen();
			//sign message
//	System.out.println("enc str " + Base64.getEncoder().encodeToString(sharedKey.getEncoded()));
			//byteMessage = JEncryptRSA.encryptRSA(Base64.getEncoder().encodeToString(sharedKey.getEncoded()), client_privExp, client_modulus);
			sentSecret = sharedKey;
			sentSecretKey = sharedKey.getEncoded();
			//sentSecretKey = Base64.getEncoder().encodeToString(sharedKey.getEncoded());
			
			//byteMessage = JEncryptRSA.encryptRSA(sharedKey.getEncoded(), client_privExp, client_modulus); //test
			byteMessage = JEncryptRSA.encryptRSA(sharedKey.getEncoded(), server_pubExp, server_modulus);
	System.out.println("sending encrypted key");
	sent1_signed = byteMessage;
	byteMessage = JEncryptRSA.encryptRSA(byteMessage, server_pubExp, server_modulus);
	sent2_encryptedPublic = byteMessage;
	
			//encrypt with server's public key
/*			byteMessage = JEncryptRSA.encryptRSA(byteMessage.toString(), pubExp, pubMod);
	System.out.println("r2 ency enc str " + byteMessage.toString());*/
			this.writeMessageToChatWindow("Sending: Session Key " + sharedKey.toString());
			this.sendRawMessage(byteMessage.length,byteMessage);
		}
	}

}
