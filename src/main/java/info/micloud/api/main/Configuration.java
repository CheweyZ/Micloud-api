package info.micloud.api.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Configuration {

	public static String CONFIG_FILE_PATH = "micloud.cfg";
	public static File CONFIG_FILE = new File(CONFIG_FILE_PATH);
	
	public static String SERVER_IP;
	public static int SERVER_PORT  = -1;
	
	public static String MYSQL_IP;
	public static int	 MYSQL_PORT = -1;
	public static String MYSQL_DB;
	public static String MYSQL_USERNAME;
	public static String MYSQL_PASSWORD;
	
	public static int	 THREAD_COUNT = 2;
	
	public static int	 SHOW_LAST_VISITS = -1;
	
	public static void onLoad(String[] args) {
		for (int i = 0;i < args.length;i++){
			String arg = args[i];
			if (arg.startsWith("-")){
				if (arg.startsWith("-config=")){
					String file = arg.substring(8);
					CONFIG_FILE_PATH = file;
					CONFIG_FILE = new File(CONFIG_FILE_PATH);
				}
			} else if (args[i].equals("help")){
				printHelp();
				return;
			}
		}
		
		if (CONFIG_FILE.exists() && CONFIG_FILE.isFile()){
			try {
				FileInputStream configFile = new FileInputStream(CONFIG_FILE);
				Scanner scanner = new Scanner(configFile);
				while (scanner.hasNextLine()){
					String line = scanner.nextLine();
					String[] parts = line.split(Pattern.quote(" "));
					if (parts.length < 2){
						continue; //invalid line, skip to next line
					}
					String key = parts[0].toLowerCase();
					String value = line.substring(key.length()+1);
					switch (key){
					case "db_user":
						MYSQL_USERNAME = value;
						break;
					case "db_pass":
						MYSQL_PASSWORD = value;
						break;
					case "db_ip":
						MYSQL_IP = value;
						break;
					case "db_port":
						MYSQL_PORT = Integer.parseInt(value);
						break;
					case "db_database":
						MYSQL_DB = value;
						break;
					case "bind_ip":
						SERVER_IP = value;
						break;
					case "bind_port":
						SERVER_PORT = Integer.parseInt(value);
						break;
					case "threads":
						THREAD_COUNT = Integer.parseInt(value);
						break;
					case "show_last_visits":
						SHOW_LAST_VISITS = Integer.parseInt(value);
					}
				}
				scanner.close();
				
				//make sure all the config is there
				if (MYSQL_USERNAME == null || MYSQL_PASSWORD == null || MYSQL_IP == null || MYSQL_DB == null || SERVER_IP == null || MYSQL_PORT < 0
						|| SERVER_PORT < 0 || SHOW_LAST_VISITS < 0){
					throw new RuntimeException("Incomplete configuration file");
				}
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
			
		} else {
			throw new RuntimeException("Configuration file not found at '"+CONFIG_FILE.getAbsolutePath()+"'");
		}
	}
	
	private static void printHelp(){
		System.out.println("--- MiCloud API Help ---");
		System.out.println("");
		System.out.println("  help:");
		System.out.println("    Displays this help menu");
		System.out.println("  -config=:");
		System.out.println("    Sets the path to the configuration file");
	}
}
