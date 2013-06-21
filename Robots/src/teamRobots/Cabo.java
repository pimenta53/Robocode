package teamRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;
import robocode.Droid;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.MessageEvent;
import robocode.StatusEvent;
import robocode.TeamRobot;
import sampleteam.Point;
import sampleteam.RobotColors;
import ontology.*;
import ontology.*;

public class Cabo extends TeamRobot implements Droid{
	
	private Move move ;
	private boolean mission=false;

	private double openness = 0.20;
	private double conscientiousness = 0.10;
	private double extraversion = 0.70;
	private double agreeableness = 0.95;
	private double neuroticism = 0.35;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
	
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	/**
	 * run:  Droid's default behavior
	 */
	public void run() {
		out.println("I'm ready sir!!!!.");
	}
	
	public void onStatus(StatusEvent e){
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}

	/**
	 * onMessageReceived:  What to do when our leader sends a message
	 */
	public void onMessageReceived(MessageEvent e) {
		// Fire at a point
		if (e.getMessage() instanceof Position) {
			mission=true;
			out.println("OK sir!!!!.");
			Position p = (Position) e.getMessage();
			// Calculate x and y to target
			double dx = p.getX() - this.getX();
			double dy = p.getY() - this.getY();
			// Calculate angle to target
			double theta = Math.toDegrees(Math.atan2(dx, dy));

			move= new Move(new Position(p.getX(),p.getY()),new Position(getX(),getY()));
			
			// go to leader
			turnRight(normalRelativeAngleDegrees(theta - getGunHeading()));
			ahead(move.move()-100);
		} // Set our colors
		 if (e.getMessage() instanceof RobotColors) {
			RobotColors c = (RobotColors) e.getMessage();

			setBodyColor(c.bodyColor);
			setGunColor(c.gunColor);
			setRadarColor(c.radarColor);
			setScanColor(c.scanColor);
			setBulletColor(c.bulletColor);
		}
		 if (e.getMessage() instanceof Attack) {
			 mission=true;
			 out.println("OK sir!!!!.");
			 Attack a = (Attack) e.getMessage();
				// Calculate x and y to target
				double dx = a.getTarget().getP().getX() - this.getX();
				double dy = a.getTarget().getP().getY() - this.getY();
				// Calculate angle to target
				double theta = Math.toDegrees(Math.atan2(dx, dy));

				turnGunRight(normalRelativeAngleDegrees(theta - getGunHeading()));
				// Fire hard!
				fire(2);

		 }
		 if (e.getMessage() instanceof Help) {
			 mission=true;
			 out.println("OK sir!!!!.");
			 Help h = (Help) e.getMessage();
				// Calculate x and y to target
				double dx = h.getP().getX() - this.getX();
				double dy = h.getP().getY() - this.getY();
				// Calculate angle to target
				double theta = Math.toDegrees(Math.atan2(dx, dy));

				turnGunRight(normalRelativeAngleDegrees(theta - getGunHeading()+h.getD()));
				// Fire hard!
				fire(3);
		}
		 if (e.getMessage() instanceof Command) {
			Command c = (Command) e.getMessage();
			 if(c.getCommand().equals("stop")) mission=false;
		 }
		 
	}

	// ataca o que o está a atacar ...
		public void onHitByBullet(HitByBulletEvent e) {
			if(!mission){
				// Set the target
				double gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
				turnGunRight(gunTurnAmt);
				fire(2.2);
			}
		}
	
		private double getEmotionIntensity(double x, double y, double z){
			return Math.sqrt(x*x + y*y + z*z)/Math.sqrt(3);
		}
		
		private void updateEmotion(Point3D newEmotion, double intensity, int tempo){

			Pcurrent = Pbase + (newEmotion.getX() * emotionI(tempo, intensity));
			Acurrent = Abase + (newEmotion.getY() * emotionI(tempo, intensity));
			Dcurrent = Dbase + (newEmotion.getZ() * emotionI(tempo, intensity));
			if (tempo == 0) t = (int) System.currentTimeMillis()/100;
		}
		
		private double emotionI(int t, double intensity){
			return intensity * Math.exp(-t*neuroticism);
		}		
}
