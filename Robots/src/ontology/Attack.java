package ontology;

public class Attack implements java.io.Serializable{

	
	private int priority;
	private Target target;
	
	public Attack( int pri, Target tg){
		priority=pri;
		target = tg;
	}
	
	public Target getTarget() {
		return target;
	}

	public void setTarget(Target target) {
		this.target = target;
	}

	public int getPriority() {
		return priority;
	}


	public void setPriority(int priority) {
		this.priority = priority;
	}

	
}
