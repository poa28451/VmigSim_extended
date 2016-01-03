package broker_collaborator;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;

import closed_loop.FuzzyLogic;
import cloudsim_inherit.VmigSimVm;
import file_manager.FuzzyWriter;
import message.MigrationMessage;
import variable.Constant;
import variable.Environment;

public class Controller {
	private ArrayList<MigrationMessage> vmQueue;
	private ArrayList<MigrationMessage> undoneVmList;
	private ArrayList<ControllerThread> workerList;
	private CountDownLatch threadLocker;
	private double highestMigTime;
	private FuzzyLogic fuzzy;
	private int threadNum;
	
	private boolean isExceedTime = false;
	
	public Controller(SimEntity srcEnt, SimEntity destEnt, ArrayList<MigrationMessage> vmQueue){
		this.vmQueue = vmQueue;
		threadNum = Environment.threadNum;
		threadLocker = new CountDownLatch(1);
		highestMigTime = Double.MIN_VALUE;
		prepareThread(srcEnt, destEnt);
		
		if(Environment.controlType == Constant.CLOSED_LOOP){
			fuzzy = new FuzzyLogic();
		}
		
		undoneVmList = new ArrayList<MigrationMessage>();
	}
	
	public void startControlling(){
		ControllerThread freeWorker;
		boolean isDone = false;
		int assignedToThread = 0;
		try {
			MigrationMessage migration;
			for(int i=0; i<vmQueue.size(); i++){
				//If there's no pending VM in the undone list, pick from the sending queue.
				if(undoneVmList.isEmpty()){
					migration = vmQueue.get(i);
				}
				else{
					//Else, pick from the undone list.
					migration = undoneVmList.remove(0);
					//Remain the index of the sending queue
					i--;
				}
				//migration = selectVmFromQueue(i);
				do{
					freeWorker = findFreeThread();
				} while(freeWorker == null);
				
				//Set the VM to the thread and start calculating				
				freeWorker.setData(migration);
				freeWorker.start();
				assignedToThread++;
				
				//If every VM is assigned to thread, start the controlling method.
				if(assignedToThread == threadNum){
					control(isDone);
					//Every thread is done running, reset the number of running job to 0.
					assignedToThread = 0;
				}
				//If there're any threads that has the migration time exceeded the limitation,
				//	stop the process of thread's job assigning.
				if(isExceedTime){
					break;
				}
			}
			isDone = true;
			synchroWorkerThreads(isDone); //Synchronize the thread for the last time.
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(0);
		}
		highestMigTime = findHighestMigTime();
	}
	
	private void control(boolean isDone) throws InterruptedException{
		if(Environment.controlType == Constant.OPEN_LOOP){
			controlByOpenLoop(isDone);
		}
		else{
			controlByClosedLoop();
		}
	}
	
	private void controlByOpenLoop(boolean isDone) throws InterruptedException{
		synchroWorkerThreads(isDone);
	}
	
	private void controlByClosedLoop() throws InterruptedException{
		for(ControllerThread t : workerList){
			t.join();
		}
		prepareForThreadAdaption();
		int newThreadNum = fuzzyCalculate();
		manageThreadAdaption(newThreadNum);
	}
	
	private MigrationMessage selectVmFromQueue(int sequence){
		//If there's no pending VM in the undone list, pick from the sending queue.
		if(undoneVmList.isEmpty()){
			return vmQueue.get(sequence);
		}
		//Else, pick from the pending list.
		return undoneVmList.remove(0);
	}
	
	/**
	 * Find the first free worker (done its current execution) and return it.
	 * @return the free worker if it is existed, null if no free worker.
	 */
	private ControllerThread findFreeThread(){
		//Concurrent style
		double least = Double.MAX_VALUE;
		ControllerThread next = null;
		for(ControllerThread worker : workerList){ 
			if(!worker.isAlive() && !worker.isTerminated() && least > worker.getNextMigrationDelay()){
				least = worker.getNextMigrationDelay();
				next = worker;
			}
		}
		return next;
		
		//Sequential style
		/*double least = Double.MAX_VALUE;
		ControllerWorker next = null;
		for(ControllerWorker worker : workerList){
			if(worker.isAlive()) return null;
			if(least > worker.getNextMigrationDelay()){
				least = worker.getNextMigrationDelay();
				next = worker;
			}
		}
		return next;*/
	}
	
	private void prepareForThreadAdaption(){
		double LowestTime = findLowestMigTime();

		if(LowestTime == Double.MAX_VALUE){
			return;
		}
		for(ControllerThread t: workerList){
			t.setMigrationTimeToLowest(LowestTime);
			//System.out.println("yyy" + t.getMigrationTime());
		}
	}
	
	private int fuzzyCalculate() throws InterruptedException{
		double totalBwMBps = 0;
		double currentTime = 0;
		double totalLeftRamMB = 0;
		
		//Find the highest clock among the threads, this time will be set to all thread after the calculation.
		currentTime = findHighestMigTime();
		if(currentTime == Double.MAX_VALUE){
			return Integer.MIN_VALUE;
		}
		//System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxx" + currentTime);
		double clock = currentTime + CloudSim.clock();
		
		//Find the current total bw used by threads, also the current migration time.
		for(ControllerThread t : workerList){
			totalBwMBps += NetworkGenerator.getBandwidthAtTimeMB(t.getThreadId(), clock);
		}
		
		//Find the total being-migrated RAM of VM (i.e. total RAM of VMs those are not migrated yet).
		for(MigrationMessage migration : vmQueue){
			VmigSimVm vm = migration.getVm();
			if(!vm.isMigratedOut()){
				totalLeftRamMB += vm.getRam();
			}
		}
		
		double need;
		if(totalBwMBps <= 0){
			need = 0;
		}
		else{
			need = totalLeftRamMB / totalBwMBps;
		}
		double predictedTime = currentTime + need;
		double error = Environment.migrationTimeLimit - predictedTime;
		double status = error * 100 / Environment.migrationTimeLimit;
		
		String log = "";
		log += "clock: " + clock + "\n";
		log += "time_predict: " + predictedTime + ", BW(MBps): " + totalBwMBps + ", leftRAM: " + totalLeftRamMB + "\n";
		log += "status: " + error;
		FuzzyWriter.appendThreadTrace(log);
		//System.out.println(log);
		
		int newThreadNum = fuzzy.evaluateResult(status, threadNum);
		
		log = "Change thread number from " + threadNum + " to " + newThreadNum + "\n";
		FuzzyWriter.appendThreadTrace(log);
		//System.out.println(log);
		//System.out.println("old: " + threadNum + " new: " + newThreadNum + " error%: " + status + "(" + error + ") predict: " + predictedTime + " totalBW: " + totalBwMBps + " leftRAM: " + totalLeftRamMB);
		//System.out.println();		
		
		return newThreadNum;
	}
	
	private void manageThreadAdaption(int threadNum){
		if(threadNum == Integer.MIN_VALUE){
			isExceedTime = true;
			return;
		}
		if(this.threadNum != threadNum){
			changeTraceFile(threadNum);	
			this.threadNum = threadNum;
			Environment.setThreadNum(threadNum);
		}
		
		double highestMigTime = findHighestMigTime();
		SimEntity srcEnt = workerList.get(0).getSrcEntity();
		SimEntity destEnt = workerList.get(0).getDestEntity();
		
		//Change the number of thread, along with adjusting the migration time to the highest one.
		prepareThread(srcEnt, destEnt, highestMigTime);
	}
	
	private void changeTraceFile(int threadNum){
		String oldname = Environment.traceFile;
		String oldThread = String.valueOf(Environment.threadNum) + "t";
		String newThread = String.valueOf(threadNum) + "t";
		String newname = oldname.replaceAll(oldThread, newThread);
		
		Environment.setTraceFile(newname);
		new NetworkGenerator(newname, threadNum);
	}
	
	/**
	 *  Prepare the threads for the calculation.
	 * @param isDone true if the threads are needed to refresh the status.
	 * @throws InterruptedException
	 */
	private void synchroWorkerThreads(boolean isDone) throws InterruptedException{
		for(ControllerThread t : workerList){
			t.join();
		}
		if(!isDone){
			ArrayList<ControllerThread> newList = new ArrayList<ControllerThread>();
			for(ControllerThread t : workerList){
				t.sendVm();
				
				int threadId = t.getThreadId();
				SimEntity srcEnt = t.getSrcEntity();
				SimEntity destEnt = t.getDestEntity();
				double nextMigrationDelay = t.getNextMigrationDelay();
				t = new ControllerThread(threadId, srcEnt, destEnt, nextMigrationDelay, threadLocker);
				newList.add(t);
			}
			workerList = newList;
		}
	}
	
	/**
	 * Initialize the threads, the number of threads depends on number defined from the user.
	 */
	private void prepareThread(SimEntity srcEnt, SimEntity destEnt){
		workerList = new ArrayList<>();
		threadLocker.countDown();
		for(int i=0; i<threadNum; i++){
			ControllerThread worker = new ControllerThread(i, srcEnt, destEnt, 0, threadLocker);
			workerList.add(worker);
		}
	}
	
	private void prepareThread(SimEntity srcEnt, SimEntity destEnt, double migrationTime){
		workerList = new ArrayList<>();
		for(int i=0; i<threadNum; i++){
			ControllerThread worker = new ControllerThread(i, srcEnt, destEnt, migrationTime, threadLocker);
			workerList.add(worker);
		}
	}
	
	private double findHighestMigTime(){
		double highest = Double.MIN_VALUE;
		for(ControllerThread t : workerList){
			if(t.getNextMigrationDelay() > highest){
				highest = t.getNextMigrationDelay();
			}
		}
		return highest;
	}
	
	private double findLowestMigTime(){
		double lowest = Double.MAX_VALUE;
		int lowestTimeIndex = 0;
		for(int i=0; i<workerList.size(); i++){
			double nextMig = workerList.get(i).getMigrationTime();
			if(nextMig < lowest && nextMig != Double.MIN_VALUE){
				lowest = nextMig;
				lowestTimeIndex = i;
			}
		}
		
		for(int i=0; i<workerList.size(); i++){
			ControllerThread t = workerList.get(i);
			if(i == lowestTimeIndex){
				//Mark the flag to the thread that has lowest migration time.
				t.setIsLowestTime(true);
			}
			else{
				//Else, add the VM in the thread to the undone list.
				undoneVmList.add(t.getVmMsg());
			}
		}
		return lowest;
	}
	
	public double getHighestMigrationTime(){
		return highestMigTime;
	}
}
