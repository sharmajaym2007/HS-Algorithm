import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/*
 * Team: 
 * Chandani Madnani (cxm152930)
 * Jay Sharma (jms140730)
 * Maitri Hitesh Jatakia (mhj150030) 
 */

public class HSMaster {
	private int pIdMaster, totalProcesses;
	boolean isCompleted = false;
	// All the processes write Message to this blocking queue to indicate that
	// they are ready for the next round
	private BlockingQueue<Message> masterBlockingQueue;

	// Master writes to this Blocking Queue collection with the start of every
	// round
	private ArrayList<BlockingQueue<Message>> queueList = new ArrayList<BlockingQueue<Message>>();
	private ArrayList<BlockingQueue<Message>> roundQueueList = new ArrayList<BlockingQueue<Message>>();

	public HSMaster(int pIdMaster, ArrayList<Integer> processIds) {
		this.pIdMaster = pIdMaster;
		totalProcesses = processIds.size();

		masterBlockingQueue = new ArrayBlockingQueue<>(totalProcesses);

		Message readyMsg;

		for (int i = 0; i < totalProcesses; i++) {
			readyMsg = new Message(processIds.get(i), Message.Type.READY, Integer.MIN_VALUE, 'X');
			masterBlockingQueue.add(readyMsg);

			queueList.add(new ArrayBlockingQueue<>(2));
			roundQueueList.add(new ArrayBlockingQueue<>(2));
		}
	}

	// Getters
	public BlockingQueue<Message> getMasterBlockingQueue() {
		return masterBlockingQueue;
	}

	public ArrayList<BlockingQueue<Message>> getQueueList() {
		return queueList;
	}

	public ArrayList<BlockingQueue<Message>> getRoundQueueList() {
		return roundQueueList;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	/**
	 * Validates if a new round should be started
	 * 
	 * @return true if message type is 'Ready' else false
	 */
	public boolean shouldStartNewRound() {
		int count = 0;
		// If size of the queue is less than total Processes simply return false
		if (masterBlockingQueue.size() < totalProcesses) {
			return false;
		}

		Message message;

		for (int i = 0; i < totalProcesses; i++) {
			try {
				message = masterBlockingQueue.take();
				// If we already know the leader than simply mark the flag as
				// completed and return false
				if (message.getType() == Message.Type.LEADER) {
					if (++count == totalProcesses) {
						isCompleted = true;
						return false;
					}
				}
				// If message type is neither Ready nor Leader than return false
				if (message.getType() != Message.Type.READY && message.getType() != Message.Type.LEADER) {
					return false;
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	/**
	 * Starts the next round by adding all the processes to the message queue
	 */
	public void beginNextRound() {
		Iterator<BlockingQueue<Message>> queueIterator = roundQueueList.iterator();

		while (queueIterator.hasNext()) {
			BlockingQueue<Message> messageQueue = queueIterator.next();
			messageQueue.add(new Message(pIdMaster, Message.Type.NEXT, Integer.MIN_VALUE, 'X'));
		}
	}

	public static void main(String[] args) {
		int totalProcesses = 0;
		int masterPId = 0;
		ArrayList<Integer> processIds = new ArrayList<Integer>();
		Scanner sc = null;
		try {
			sc = new Scanner(new File("inputData.txt"));
			totalProcesses = Integer.parseInt(sc.nextLine());
			// Read all the processes and split it by whitespace
			String processes[] = sc.nextLine().split("\\s+");
			if (processes.length != totalProcesses) {
				System.err.println("Please specify " + totalProcesses + " process ids");
				return;
			}
			for (int i = 0; i < processes.length; i++) {
				processIds.add(Integer.parseInt(processes[i]));
			}

		} catch (FileNotFoundException e) {
			System.err.println(
					"We were not able to find your file. Please check if the file exists in the project folder");
			return;
		} finally {
			sc.close();
		}

		// Create master controller and slave processes
		HSMaster master = new HSMaster(masterPId, processIds);
		HSSlaveThread[] processes = new HSSlaveThread[totalProcesses];

		for (int i = 0; i < totalProcesses; i++) {
			processes[i] = new HSSlaveThread(processIds.get(i));
		}

		for (int i = 0; i < totalProcesses; i++) {
			// Add a incoming queue to the process
			processes[i].setInputQueue(master.getQueueList().get(i));

			if (i == 0) {
				processes[i].setPredecessor(processes[(totalProcesses - 1) % totalProcesses]);
			} else {
				processes[i].setPredecessor(processes[(i - 1) % totalProcesses]);
			}

			processes[i].setSuccessor(processes[(i + 1) % totalProcesses]);
			processes[i].getOutputMessageList().clear();

			Message msg1 = new Message(processIds.get(i), Message.Type.OUT, 1, 'L');
			Message msg2 = new Message(processIds.get(i), Message.Type.OUT, 1, 'R');

			processes[i].getOutputMessageList().add(msg1);
			processes[i].getOutputMessageList().add(msg2);

			processes[i].setRoundQueue(master.getRoundQueueList().get(i));
		}

		// Start all threads
		Thread[] slaveThreads = new Thread[totalProcesses];
		for (int i = 0; i < totalProcesses; i++) {
			processes[i].setMasterQueue(master.getMasterBlockingQueue());
			slaveThreads[i] = new Thread(processes[i]);
			slaveThreads[i].start();
		}

		// Validate if HS algorithm is completed, if not then start the next
		// round
		while (!master.isCompleted()) {
			if (master.shouldStartNewRound()) {
				master.beginNextRound();
			}
		}

		for (int i = 0; i < totalProcesses; i++) {
			slaveThreads[i].interrupt();
		}

		// Wait for child threads to get completed
		for (int i = 0; i < slaveThreads.length; i++) {
			try {
				slaveThreads[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		System.out.println("HS Algorithm finished");
	}
}
