package chatapp;
public class CLE {

	public static void main(String[] args)
	{
		ChatApp server = new ChatApp("localhost", 10901, "server", "keyGenerationSeed", ChatApp.designation.server);
		ChatApp client = new ChatApp("localhost", 10901, "client", "keyGenerationSeed", ChatApp.designation.client);
		
		server.start();
		client.start();
	}
}
