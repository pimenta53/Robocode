package ontology;

public class Target implements java.io.Serializable{

	private String name;
	private double power;
	private String type;
	private Position p;
	
	public Target (String n, double pm, String ty, Position p){
		name=n;
		power=pm;
		type=ty;
		this.p=p;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getPower() {
		return power;
	}

	public void setPower(float power) {
		this.power = power;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Position getP() {
		return p;
	}

	public void setP(Position p) {
		this.p = p;
	}
	
	
}
