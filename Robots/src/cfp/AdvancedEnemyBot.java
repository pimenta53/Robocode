package cfp;

import robocode.Robot;
import robocode.ScannedRobotEvent;

public class AdvancedEnemyBot extends EnemyBot{

	private double x;
	private double y;
	
	public AdvancedEnemyBot(){
		reset();
	}
	
	public double getX(){ return this.x;}
	public double getY(){ return this.y;}
	
	public void reset(){
		super.reset();
		this.x = 0.0; this.y = 0.0;
	}
	
	public void update(ScannedRobotEvent e, Robot robot){
		super.update(e);
		double absBearingDeg = robot.getHeading() + e.getBearing();
		if (absBearingDeg < 0) absBearingDeg += 360;
		// yes, you use the _sine_ to get the X value because 0 deg is North
		x = robot.getX() + Math.sin(Math.toRadians(absBearingDeg)) * e.getDistance();
		// yes, you use the _cosine_ to get the Y value because 0 deg is North
		y = robot.getY() + Math.cos(Math.toRadians(absBearingDeg)) * e.getDistance();
	}
	
	public double getFutureX(long when){
		return x + Math.sin(Math.toRadians(getHeading())) * getVelocity() * when;
	}
	
	public double getFutureY(long when){
		return y + Math.cos(Math.toRadians(getHeading())) * getVelocity() * when;
	}
}
