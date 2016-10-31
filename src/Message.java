
/*
 * Message class represents a Message which has process_ID, Message_Type, count of hops, Direction from which it is coming
 * */
public class Message {
	private int pId;
	private double hopCount;
	// Direction can be left, right or undefined
	private char direction;

	// Specifies the type of the message
	public enum Type {
		READY, NEXT, IN, OUT, LEADER;
	}

	private Type type;

	public Message(int pId, Type type, double hopCount, char direction) {
		this.pId = pId;
		this.type = type;
		this.hopCount = hopCount;
		this.direction = direction;
	}

	// Getters and Setters

	public int getpId() {
		return pId;
	}

	public void setpId(int pId) {
		this.pId = pId;
	}

	public double getHopCount() {
		return hopCount;
	}

	public void setHopCount(double hopCount) {
		this.hopCount = hopCount;
	}

	public char getDirection() {
		return direction;
	}

	public void setDirection(char direction) {
		this.direction = direction;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
}