package variable;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FilePathManager {
	public static String inputPath;
	private static String outputDirectory;
	private static int experimentRound;
	
	private static final String resultPath = "result/";
	private static final String networkPath = "network/";
	private static final String logPath = "logfile/";
	private static final String fuzzyPath = "fuzzy/";
	
	private static final String resultFileName = "result";
	private static final String networkFileName = "network";
	private static final String logFileName = "log";
	private static final String fuzzyFileName = "fuzzyresult";
	
	private static final String roundWord = "-round";
	private static final String logExtension = ".txt";
	private static final String resultExtension = ".json";

	private static final String closedLoopPath = "run_simulation/closed-loop/";
	private static final String bw1tFilename = "bandwidth-1t.txt";
	private static final String fuzzyRuleFilename = "fuzzy-rule.txt";

	public static void setInputPath(String inputPath) {
		FilePathManager.inputPath = inputPath;
	}
	public static void setOutputDirectory(String outputDirectory) {
		//String simOutputDir = addSlashAtEnd(outputDirectory) + generateOutputTimeStamp();
		prepareOutputDirectory(outputDirectory);
		FilePathManager.outputDirectory = addSlashAtEnd(outputDirectory);
	}
	
	public static void setExperimentRound(int experimentRound){
		FilePathManager.experimentRound = experimentRound;
	}
	
	private static void prepareOutputDirectory(String mainOutputDir){		
		mainOutputDir = addSlashAtEnd(mainOutputDir);
		
		//Create output main directory
		createDirectory(mainOutputDir);
		
		//Create simulation result path
		//createDirectory(mainOutputDir + resultPath);
		
		//Create network result path
		createDirectory(mainOutputDir + networkPath);
		
		//Create log file path
		createDirectory(mainOutputDir + logPath);
		
		//Create fuzzy log path
		createDirectory(mainOutputDir + fuzzyPath);
	}
	
	private static String addSlashAtEnd(String path){
		if(!path.endsWith("/") && !path.endsWith("\\")){
			path += "/";
		}
		return path;
	}
	
	/**
	 * Create the directory specified (including its parents)
	 * @param dirPath the directory being created 
	 */
	private static void createDirectory(String dirPath){
		File dir = new File(dirPath);
		dir.mkdirs();
	}
	
	/**
	 * Create the non-existed parent directories of the file specified
	 * @param filePath The file path which its parent directories are being created
	 */
	@SuppressWarnings("unused")
	private static void createFileParentDirectory(String filePath){
		int pos1 = filePath.lastIndexOf('/');
		int pos2 = filePath.lastIndexOf('\\');

		//If both was -1, it means filePath has no directory included
		if(pos1 != -1 || pos2 != -1){
			int usedPos = pos1 > pos2 ? pos1 : pos2;
			String dirPath = filePath.substring(0, usedPos);
			File dir = new File(dirPath);
			dir.mkdirs();
		}
	}
	
	/**
	 * Create timestamp just in case of wanting to use it
	 * @return timestamp in the format "2558-02-06_22-56-14"
	 */
	@SuppressWarnings("unused")
	private static String generateOutputTimeStamp(){
		//Ex. 2558-02-06_22-56-14
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Calendar calendar = Calendar.getInstance();
		String timeStamp = dateFormat.format(calendar.getTime());
		return timeStamp;
	}
	
	public static String getResultFilePath(){
		String resultFullName = resultFileName + roundWord + experimentRound + resultExtension;
		return outputDirectory + resultPath + resultFullName;
	}
	
	public static String getNetworkFilePath(){
		String networkFullName = networkFileName + roundWord + experimentRound + logExtension;
		return outputDirectory + networkPath + networkFullName;
	}
	
	public static String getLogFilePath(){
		String logFullName = logFileName + roundWord + experimentRound + logExtension;
		return outputDirectory + logPath + logFullName;
	}
	
	public static String getFuzzyFilePath(){
		String fuzzyFullName = fuzzyFileName + roundWord + experimentRound + logExtension;
		return outputDirectory + fuzzyPath + fuzzyFullName;
	}
	
	public static String getFuzzyRuleFilePath(){
		String fuzzyPath = closedLoopPath + fuzzyRuleFilename;
		return fuzzyPath;
	}
	
	public static String getInitialBwFilePath(){
		String fuzzyPath = closedLoopPath + bw1tFilename;
		return fuzzyPath;
	}
}
