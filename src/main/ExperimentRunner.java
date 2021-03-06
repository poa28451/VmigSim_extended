package main;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import container.Parameters;
import file_manager.FuzzyWriter;
import file_manager.JsonReader;
import file_manager.LogWriter;
import file_manager.NetworkWriter;
import file_manager.ResultWriter;
import variable.Environment;
import variable.FilePathManager;

public class ExperimentRunner {	
	private int experimentRounds = 1;
	public static int currentRound = 1;
	
	private static String inDir = "experiment input files/";
	private static String inName = "5-parallel-closed-off-fifo.json";
	private static String outDir = "output3/";
	private static String outName = inName.substring(0, inName.indexOf(".json"));
	
	private static String input = inDir + inName;
	private static String output = outDir + outName;
	private static int round = 10;
	
	private JsonReader decoder;
	
	public ExperimentRunner(String inputPath, String outputPath, int experimentRounds){
		FilePathManager.setInputPath(inputPath);
		FilePathManager.setOutputDirectory(outputPath);
		decoder = new JsonReader();
		this.experimentRounds = experimentRounds;
	}
	
	public void runExperiment(){
		try {
			Parameters param = decoder.readInputFile(FilePathManager.inputPath);
			for(;currentRound <= experimentRounds; currentRound++){
				FilePathManager.setExperimentRound(currentRound);
				runExperiment(param);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void runExperiment(Parameters param) throws FileNotFoundException{
		PrintStream stream = prepareLogPath();
		System.setOut(stream);
		
		VmigSimCore vmigsim = new VmigSimCore();
		vmigsim.startSimulation(param);
		
		LogWriter.writeLogToFile(Environment.migrationResult);
		
		//ResultWriter.writeResultToFile(Environment.migrationResult);
		
		NetworkWriter.writeNetworkToFile();
		
		FuzzyWriter.writeFuzzyLogToFile();
		
		stream.close();
	}
	
	private PrintStream prepareLogPath(){
		String logPath = FilePathManager.getLogFilePath();
		FileOutputStream fileStream;
		try {
			fileStream = new FileOutputStream(logPath);
			PrintStream stream =  new PrintStream(fileStream);
			return stream;
		} catch (FileNotFoundException e) {
			System.out.println("Output file's path not found.");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}
	
	public static void main(String args[]){
		/*if(args.length == 3){
			String inputPath = args[0];
			String outputPath = args[1];
			int experimentRound = Integer.valueOf(args[2]);
			
			ExperimentRunner runner = new ExperimentRunner(inputPath, outputPath, experimentRound);
			runner.runExperiment();
		}
		else{
			showHelp();
		}*/

		String inputPath = input;
		String outputPath = output;
		int experimentRound = round;
		
		ExperimentRunner runner = new ExperimentRunner(inputPath, outputPath, experimentRound);
		runner.runExperiment();
	}
	
	private static void showHelp(){
		System.out.println("VmigSim need 3 arguments for simulating");
		System.out.println("\t1. Input file path: /path/to/file.json");
		System.out.println("\t1. Output directory: /path/to/directory");
		System.out.println("\t1. Simulation round: ex. 10");
		System.out.println("Usage: java -jar vmigsim.jar <input path> <output dir> <round>");
	}
}
