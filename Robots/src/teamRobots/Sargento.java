package teamRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.util.ArrayList;

import ontology.Attack;
import ontology.Command;
import ontology.Help;
import ontology.Move;
import ontology.Point3D;
import ontology.Position;
import ontology.Target;

import robocode.MessageEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.TeamRobot;
import sampleteam.RobotColors;

public class Sargento extends TeamRobot{

	private Move move ;
	private ArrayList<Target> enemies = new ArrayList<Target> ();
	private Target tg;
	private String target;
	private int count;
	double gunTurnAmt; // How much to turn our gun when searching

	private double openness = 0.85;
	private double conscientiousness = 0.90;
	private double extraversion = 0.20;
	private double agreeableness = 0.65;
	private double neuroticism = 0.45;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
	
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	public void run(){
		
		setAdjustGunForRobotTurn(true); // Keep the gun still when we turn
		gunTurnAmt = 10; // Initialize gunTurn to 10
		
		
		while (true) {
			if(count>=5) target=null;
			count++;
			enemies.clear();
			turnGunRight(gunTurnAmt);
			// Keep track of how long we've been looking
			
			// If we've haven't seen our target for 1 turns, look left
			if (count > 1) {
				gunTurnAmt = -10;
			}
			// If we still haven't seen our target for 2 turns, look right
			if (count > 2) {
				gunTurnAmt = 10;
			}
			count++;
		}
	}

	public void onStatus(StatusEvent e){
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}
	
	public void onMessageReceived(MessageEvent e) {
		// Fire at a point
		if (e.getMessage() instanceof Position) {
			Position p = (Position) e.getMessage();
			// Calculate x and y to target
			double dx = p.getX() - this.getX();
			double dy = p.getY() - this.getY();
			// Calculate angle to target
			double theta = Math.toDegrees(Math.atan2(dx, dy));

			move= new Move(new Position(p.getX(),p.getY()),new Position(getX(),getY()));
			
			// Turn gun to target
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
			 Attack a = (Attack) e.getMessage();
		 }
		 if (e.getMessage() instanceof Help) {
			 Help p = (Help) e.getMessage();
		 }
		 if (e.getMessage() instanceof Command) {
			 Command p = (Command) e.getMessage();
		 }
	}
	
	
	
	public void onScannedRobot(ScannedRobotEvent e) {
		
		// Don't fire on teammates
		if (isTeammate(e.getName())) {
			return;
		}	
	
		target = e.getName();
		
		// Calculate enemy bearing
		double enemyBearing = this.getHeading() + e.getBearing();
		// Calculate enemy's position
		double enemyX = getX() + e.getDistance() * Math.sin(Math.toRadians(enemyBearing));
		double enemyY = getY() + e.getDistance() * Math.cos(Math.toRadians(enemyBearing));
		
		
		Target tg = new Target(target,e.getEnergy(),e.getClass().getName(),new Position(enemyX,enemyY));
		
		enemies.add(tg);
		
		
		// se é o alvo que definiu ...
		// This is our target.  Reset count (see the run method)
				count = 0;
	
			if(target!=null){
				// Perseguilo se tiver a mais de 250 ...
				if (e.getDistance() > 200 && e.getDistance() < 400) {
					double gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));

					turnGunRight(gunTurnAmt); // Try changing these to setTurnGunRight,
					turnRight(e.getBearing()); // and see how much Tracker improves...
					// (you'll have to make Tracker an AdvancedRobot)
					ahead(e.getDistance() - 120);
					return;
				}

				// se está perto entre 250 e 120
				double gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
				turnGunRight(gunTurnAmt);
				fire(2.5);

				//se tiver perto de 100 unidades dispara e ainda se aproxima, partindo no 1 para 1
				if (e.getDistance() <= 150) {
					if (e.getEnergy() > 16) {
						fireBullet(3);
					} else if (e.getEnergy() > 10) {
						fireBullet(2);
					} else if (e.getEnergy() > 4) {
						fire(1);
					} else if (e.getEnergy() > 2) {
						fire(.5);
					} else if (e.getEnergy() > .4) {
						fire(.1);
					}
					turnRight(e.getBearing());
					ahead(5);
					
			}
		}
		else target();
	scan();		
		
	}

	public Target target(){
		Move m = null;
		double power=10000;
		Target tg=null;
			for(Target t : enemies){
				m = new Move(new Position(getX(),getY()),new Position(t.getP().getX(),t.getP().getX()));
				if(t.getPower()<power && m.move()<300){
					power=t.getPower();	
					tg=t;
				}
			}
		return tg;
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
