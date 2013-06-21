package ontology;
import java.lang.Math;

public class Move implements java.io.Serializable{

	private Position start;
	private Position end;
	
	public Move(Position s, Position e){
		start=s;
		end=e;
	}

	public Position getStart() {
		return start;
	}

	public void setStart(Position start) {
		this.start = start;
	}

	public Position getEnd() {
		return end;
	}

	public void setEnd(Position end) {
		this.end = end;
	}
	
	
	public double move(){
		return Math.sqrt( (end.getX() - start.getX()) * (end.getX() - start.getX()) + (end.getY() - start.getY()) * (end.getY() - start.getY())); 
	}
	
	public double move(double x1, double y1,double x2, double y2){
		return  Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
		
	}
}
