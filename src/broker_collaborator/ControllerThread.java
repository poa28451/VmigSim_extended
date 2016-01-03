package broker_collaborator;

import java.util.concurrent.CountDownLatch;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;

import cloudsim_inherit.VmigSimVm;
import message.MigrationMessage;
import variable.Constant;

public class ControllerThread extends Thread{
	private SimEntity srcEnt, destEnt;
	private MigrationMessage vmMsg;
	private final CountDownLatch lock;
	private boolean isTerminated;
	private double nextMigrationDelay = 0;
	private int threadId;
	
	private double migrationTime = 0;
	private boolean isLowestTime;
	
	//private double migrationTime = 0;
	
	public ControllerThread(int threadId, SimEntity srcEnt, SimEntity destEnt, double nextMigrationDelay, CountDownLatch lock){
		super(String.valueOf(threadId));
		this.threadId = threadId;
		this.srcEnt = srcEnt;
		this.destEnt = destEnt;
		this.nextMigrationDelay = nextMigrationDelay;
		this.lock = lock;
		isTerminated = false;
		
		isLowestTime = false;
	}
	
	public void setData(MigrationMessage migration){
		vmMsg = migration;
		migration.getVm().setMigratedOut(true);
	}
	
	public void run() {
		MigrationManager migManager = new MigrationManager();
		MigrationCalculator calculator = new MigrationCalculator(threadId);
		
		migManager.setMigrationData(vmMsg);
		//MigrationMessage msg;
		do{
			double currentClock = CloudSim.clock();
			
			vmMsg = migManager.manageMigration(currentClock + nextMigrationDelay);
			//msg.setSendClock(nextMig);
			double migrationTime = calculator.calculateMigrationTime(vmMsg, nextMigrationDelay + currentClock);
			
			this.migrationTime += migrationTime;
			//Stop sending if the time exceeded the limit
			if(migrationTime == Double.MIN_VALUE){
				break;
			}
			
			/*nextMigrationDelay += migrationTime;
			updateMigrationTime(vm, migrationTime);
			try {
				//To prevent the event list from the race conditions occurred by threads
				synchronized (lock) {
					lock.await();
					CloudSim.send(srcEnt.getId(), destEnt.getId(), 
							nextMigrationDelay + migrationTime, 
							Constant.SEND_VM_MIGRATE, 
							vm);
					lock.countDown();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}*/
		} while(!vmMsg.isLastMigrationMsg());
		isTerminated = true;
	}
	
	public void sendVm(){
		if(migrationTime != Double.MIN_VALUE){
			nextMigrationDelay += migrationTime;
			try {
				//To prevent the event list from the race conditions occurred by threads
				synchronized (lock) {
					lock.await();
					CloudSim.send(srcEnt.getId(), destEnt.getId(), 
							nextMigrationDelay, 
							Constant.SEND_VM_MIGRATE, 
							vmMsg);
					lock.countDown();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(1);
			}
			updateDoneMigration(vmMsg, migrationTime);
		}
	}
	
	public void setMigrationTimeToLowest(double desireTime){
		if(!isLowestTime){
			MigrationCalculator calculator = new MigrationCalculator(threadId);
			double sizeAbleToSend = calculator.calculateMigrationSize(vmMsg, nextMigrationDelay, desireTime);
			updateUndoneMigration(vmMsg, desireTime, sizeAbleToSend);
		}
		else{
			updateDoneMigration(vmMsg, desireTime);
		}
		nextMigrationDelay += desireTime;
		try {
			//To prevent the event list from the race conditions occurred by threads
			synchronized (lock) {
				lock.await();
				CloudSim.send(srcEnt.getId(), destEnt.getId(), 
						nextMigrationDelay, 
						Constant.SEND_VM_MIGRATE, 
						vmMsg);
				lock.countDown();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	private void updateDoneMigration(MigrationMessage msg, double transferTime){
		VmigSimVm vm = msg.getVm();
		
		msg.setMigrationTime(transferTime);
		vm.updateMigrationTime(transferTime);
		
		//if(msg.isLastMigrationMsg()){
			//Downtime = last transferring time
		vm.setDowntime(transferTime);
		//}
	}
	
	private void updateUndoneMigration(MigrationMessage msg, double transferTime, double sentSize){
		VmigSimVm vm = msg.getVm();
		
		msg.setMigrationTime(transferTime);
		vm.updateMigrationTime(transferTime);
		vm.updateTransferredSize(sentSize);
		
		vmMsg.setLastMigrationMsg(false);
		vm.setDowntime(transferTime);
	}
	
	public int getThreadId(){
		return threadId;
	}
	
	public boolean isTerminated(){
		return isTerminated;
	}
	
	public double getNextMigrationDelay(){
		return nextMigrationDelay;
	}
	
	public SimEntity getSrcEntity(){
		return srcEnt;
	}
	
	public SimEntity getDestEntity(){
		return destEnt;
	}
	
	public MigrationMessage getVmMsg(){
		return vmMsg;
	}
	
	public CountDownLatch getLock(){
		return lock;
	}
	
	public double getMigrationTime(){
		return migrationTime;
	}
	
	public void setIsLowestTime(boolean bool){
		isLowestTime = bool;
	}
}
