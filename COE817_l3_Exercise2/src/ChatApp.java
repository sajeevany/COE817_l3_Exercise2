import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatApp implements Runnable{
	
	public enum designation{server, client};
	
	private String hostToConnect, alias;
	private int port, myPublicKey, myPrivateKey, mySessionKey;
	private ServerSocket sSocket;
	private DataOutputStream dataOutStream;
	private DataInputStream dataInStream;
	designation myDesignation;
	Thread cThread;
	
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
		try {
			switch(myDesignation)
			{
				case server:
					openPort();
					break;
				case client:
					break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	
	public void run()
	{
		Socket mySocket;
		
		//start GUI/UI
				
		
		try {
			while(true)
			{
				if (this.myDesignation == this.myDesignation.server)
				{
					mySocket = sSocket.accept();
				} 
				else
				{
					mySocket =  new Socket(this.hostToConnect, this.port);
				}
				dataOutStream = new DataOutputStream(mySocket.getOutputStream());
				dataInStream = new DataInputStream(mySocket.getInputStream());
				
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
