package ar.edu.itba.pod.tp.player;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ar.edu.itba.pod.tp.interfaces.Referee;

/**
 * Hello world!
 *
 */
public class PlayerApp 
{
	public static int loop;
	
	public static AtomicInteger asServer = new AtomicInteger(0);
	public static AtomicInteger asClient = new AtomicInteger(0);
	
	private final static Lock lock = new ReentrantLock();
	
	public static String name;

    public static void main( String[] args ) throws ParseException
    {
		final CommandLine cmdLine = parseArguments(args);
		final int port = Integer.valueOf(cmdLine.getOptionValue(PORT_L, PORT_D));
		final String host = cmdLine.getOptionValue(HOST_L, HOST_D);
		name = cmdLine.getOptionValue(NAME_L);

		if (cmdLine.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("player", options);
			System.exit(0);
		}
		try {
			System.out.println("Registering player " + name + " on referee: " + host + ":" + port);
			Registry registry = LocateRegistry.getRegistry(host, port);
			Referee referee = (Referee) registry.lookup("referee");

			// create the player server
			PlayerServer server = new PlayerServer(name);
			System.out.println("Player ready to play");

			server.init(referee);
			loop = server.total;
			System.out.println("EMPEZAMOS!! el total de requests es: " + PlayerApp.loop);

			/* Utilizamos 3 threads para distribuir las llamadas a los players
			 * */
			ExecutorService executor = Executors.newFixedThreadPool(10);
		    for (int i = 0; i < 3; i++) {
		      Runnable worker = new PlayThread(server);
		      executor.execute(worker);
		    }
		    // This will make the executor accept no new threads
		    // and finish all existing threads in the queue
		    executor.shutdown();
		    // Wait until all threads are finish
		    while( ! executor.isTerminated() ){}
				
			System.out.println("salio!");
			System.exit(0);
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("Client exception: " + e.toString());			
			System.err.println("PERDI!");
			System.exit(1);
		}    
	}
	
	private static CommandLine parseArguments(String[] args) throws ParseException
	{
		CommandLineParser parser = new BasicParser();
		try {
			// parse the command line arguments
			return parser.parse(options, args, false);
		}
		catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			throw exp;
		}	
	}
	
	private static Options createOptions()
	{
		final Options result = new Options();
		final Option name = OptionBuilder.withArgName(NAME_S).withLongOpt(NAME_L).hasArg().withDescription("Player name").isRequired(true).create(NAME_S);
		result.addOption(name);
		result.addOption(HOST_S, HOST_L, true, "Referee server host");
		result.addOption(PORT_S, PORT_L, true, "Referee server port");
		result.addOption("help", false, "Help");
		return result;
	}

	private static final String PORT_L = "port";
	private static final String PORT_S = "p";
	private static final String PORT_D = "7242";
	private static final String HOST_L = "host";
	private static final String HOST_S = "h";
	private static final String HOST_D = "localhost";
	private static final String NAME_L = "name";
	private static final String NAME_S = "n";
	private static Options options = createOptions();
}
