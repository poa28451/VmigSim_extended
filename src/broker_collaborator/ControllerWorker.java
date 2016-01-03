package broker_collaborator;

import org.cloudbus.cloudsim.core.SimEntity;

import message.MigrationMessage;

public class ControllerWorker {
	private SimEntity srcEnt, destEnt;
	private int workerId;
	private double nextMigDelay;
	private boolean isFree;
	
	private MigrationMessage migratedVm;

	public ControllerWorker(int workerId, SimEntity srcEnt, SimEntity destEnt, double nextMigrationDelay){
		this.workerId = workerId;
		this.srcEnt = srcEnt;
		this.destEnt = destEnt;
		this.nextMigDelay = nextMigrationDelay;
		isFree = true;
	}
	
	public void setData(MigrationMessage migratedVm){
		this.migratedVm = migratedVm;
		//May be it should be set as false? (the old one is set to true because it's synchronized.
		//   the VM is guaranteed to be migrated when got set into the thread) 
		migratedVm.getVm().setMigratedOut(true);
	}
}
