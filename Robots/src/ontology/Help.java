package ontology;

public class Help implements java.io.Serializable{

	private String name;
	private Position p;
	private int priority;
	private double d;
	
	public Help(String na, Position pt, int pr, double d){
		name=na;
		p=pt;
		priority=pr;
		this.d=d;
	}

	
	
	public double getD() {
		return d;
	}



	public void setD(double d) {
		this.d = d;
	}



	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Position getP() {
		return p;
	}

	public void setP(Position p) {
		this.p = p;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	
}
