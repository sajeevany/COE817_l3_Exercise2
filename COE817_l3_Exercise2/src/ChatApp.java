import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

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
	/*private DataOutputStream dataOutStream;
	private DataInputStream dataInStream;*/
	private BufferedReader  bR;
	private  BufferedWriter bW;
	designation myDesignation;
	Thread cThread;
	
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
						sendMessage(event.getActionCommand());
						userText.setText(" ");
					}
				});
		add(userText, BorderLayout.SOUTH);
		chatWindow = new JTextArea();
		chatWindow.setEditable(false);
		JScrollPane cWin = new JScrollPane(chatWindow);
		cWin.setPreferredSize(new Dimension(200,100));
		add(cWin, BorderLayout.NORTH);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
	}
	
	public void sendMessage(String message)
	{
		try {
			/*dataOutStream.writeBytes(message);*/
			bW.write(message);
			bW.newLine();
			bW.flush();
			writeMessageToChatWindow("[ " + ((this.myDesignation == designation.server) ? "server " : "client")  +"]  " + message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		String message = " You are now connected !";
		sendMessage(message);
		do{
			try{
				message = bR.readLine();
				writeMessageToChatWindow("[ " + ((this.myDesignation == designation.server) ? "client" : "server")  +"]  " + message);
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
				/*dataOutStream = new DataOutputStream(mySocket.getOutputStream());
				dataInStream = new DataInputStream(mySocket.getInputStream());*/
				bR = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
				bW = new BufferedWriter(new OutputStreamWriter(mySocket.getOutputStream()));
				
				chat();
				closeApp();
				//Wait for connection/open connection
				//Handshake
					//exchange public keys
					//3 stage handshake
				
				//wait for action and act on action
					//sign message with private key of local app
					//encrypt message with public key of target client
			}
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

}
