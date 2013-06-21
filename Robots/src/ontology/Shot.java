package ontology;

import java.io.Serializable;

import robocode.ScannedRobotEvent;

public class Shot implements Serializable{


	private ScannedRobotEvent e;

	public Shot(ScannedRobotEvent e){
		this.e=e;
	}
	
	public ScannedRobotEvent getE() {
		return e;
	}

	public void setE(ScannedRobotEvent e) {
		this.e = e;
	}
    
        
}
