import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
/*
 HSSlaveThread Class is the class that represents each process thread/node.
 run() method implements HS Algorithm
 */

public class HSSlaveThread implements Runnable {

	private BlockingQueue<Message> inputQueue, masterQueue, roundQueue;
	private boolean isLeaderFound, leftWriteDone, rightWriteDone;
	private int leaderId, phase, pId;
	private HSSlaveThread predecessor, successor;

	private ArrayList<Message> outputMessageList = new ArrayList<Message>();

	public HSSlaveThread(int pId) {

		this.pId = pId;
		leaderId = Integer.MIN_VALUE;
		phase = 0;

	}

	public ArrayList<Message> getOutputMessageList() {
		return outputMessageList;
	}

	public void setMasterQueue(BlockingQueue<Message> masterQueue) {
		this.masterQueue = masterQueue;
	}

	public void setInputQueue(BlockingQueue<Message> inputQueue) {
		this.inputQueue = inputQueue;
	}

	public BlockingQueue<Message> getInputQueue() {
		return inputQueue;
	}

	public void setLeftWriteDone(boolean leftWriteDone) {
		this.leftWriteDone = leftWriteDone;
	}

	public void setRightWriteDone(boolean rightWriteDone) {
		this.rightWriteDone = rightWriteDone;
	}

	public void setPredecessor(HSSlaveThread predecessor) {
		this.predecessor = predecessor;
	}

	public void setSuccessor(HSSlaveThread successor) {
		this.successor = successor;
	}

	public void setRoundQueue(BlockingQueue<Message> roundQueue) {
		this.roundQueue = roundQueue;
	}

	public void run() {
		while (true) {

			Message message = null;
			try {
				message = roundQueue.take();
			} catch (InterruptedException e1) {
				System.out.println("Process with Id " + pId + " terminated");
				return;
			}
			if (message.getType() == Message.Type.NEXT) {

				Iterator<Message> iterator = outputMessageList.iterator();
				Message sendMessage;
				while (iterator.hasNext()) {
					try {
						sendMessage = iterator.next();
						if (sendMessage.getDirection() == 'L') {

							successor.getInputQueue().put(sendMessage);

						} else if (sendMessage.getDirection() == 'R') {

							predecessor.getInputQueue().put(sendMessage);

						}

						if (sendMessage.getType() == Message.Type.LEADER) {
							break;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				synchronized (predecessor) {
					predecessor.setRightWriteDone(true);
					predecessor.notify();
				}

				synchronized (successor) {
					successor.setLeftWriteDone(true);
					successor.notify();
				}

				outputMessageList.clear();

				// Waiting till successor and predecessor are done writing
				while (true) {
					synchronized (this) {
						if (leftWriteDone && rightWriteDone) {
							break;
						}
						try {
							this.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}

				leftWriteDone = false;
				rightWriteDone = false;

				int sameInCount = 0;
				Message incomingMessage = null;
				

				if (!inputQueue.isEmpty()) {
					while (inputQueue.size() > 0) {
						try {
							incomingMessage = inputQueue.take();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						switch (incomingMessage.getType()) {

						case IN: {
							if (incomingMessage.getpId() != pId) {
													outputMessageList.add(new Message(incomingMessage.getpId(), Message.Type.IN, Integer.MIN_VALUE,
										incomingMessage.getDirection()));
							} else if (incomingMessage.getpId() == pId) {
								if (++sameInCount == 2) {
									phase++;
									outputMessageList.add(new Message(pId, Message.Type.OUT, Math.pow(2, phase), 'L'));
									outputMessageList.add(new Message(pId, Message.Type.OUT, Math.pow(2, phase), 'R'));
								}
							}
						}
													break;
						case OUT: {
							if (incomingMessage.getpId() > pId) {

								if (incomingMessage.getHopCount() > 1) {
									if (incomingMessage.getDirection() == 'R') {
																			outputMessageList.add(new Message(incomingMessage.getpId(), Message.Type.OUT, incomingMessage.getHopCount() - 1,
												'R'));
									} else if (incomingMessage.getDirection() == 'L') {
																				outputMessageList.add(new Message(incomingMessage.getpId(), Message.Type.OUT, incomingMessage.getHopCount() - 1,
												'L'));
									}
								} else if (incomingMessage.getHopCount() == 1) {
									if (incomingMessage.getDirection() == 'R') {
										outputMessageList.add(new Message(incomingMessage.getpId(), Message.Type.IN, Integer.MIN_VALUE, 'L'));
									} else if (incomingMessage.getDirection() == 'L') {
										outputMessageList.add(new Message(incomingMessage.getpId(), Message.Type.IN, Integer.MIN_VALUE, 'R'));
									}
								}
							} else if (incomingMessage.getpId() < pId) {

							} else {
								if (!isLeaderFound) {
									isLeaderFound = true;
									leaderId = pId;
								System.out.println("Process with Id " + pId + " is the leader");
									outputMessageList.add(new Message(pId, Message.Type.LEADER, 1, 'L'));
								}

							}
						}
						break;
						case LEADER: {
							isLeaderFound = true;
							leaderId = incomingMessage.getpId();
							outputMessageList.add(incomingMessage);
						}
							break;
						default:
							break;
						}
					}
				}
			}

			Message ready = null;
			if (isLeaderFound) {
				ready = new Message(pId, Message.Type.LEADER, Integer.MIN_VALUE, 'X');
			} else {
				ready = new Message(pId, Message.Type.READY, Integer.MIN_VALUE, 'X');
			}

			// sending token to neighbors
			try {
				masterQueue.put(ready);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
