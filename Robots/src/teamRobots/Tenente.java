package teamRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import robocode.HitByBulletEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.TeamRobot;
import robocode.util.Utils;
import sampleteam.RobotColors;
import ontology.*;


public class Tenente extends TeamRobot{

	
	private String target;
	private ArrayList<String> team= new ArrayList<String>();
	private ArrayList<Target> enemies = new ArrayList<Target> (); 
	private String tactics;
	//vari�vel que indica o canto para onde se dirige
	private int corner = 0;
	private Target rival;
	private int rv=0;

	private double openness = 0.89;
	private double conscientiousness = 0.95;
	private double extraversion = 0.60;
	private double agreeableness = 0.70;
	private double neuroticism = 0.15;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
	
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	public void run() {
		// Prepare RobotColors object
		RobotColors c = new RobotColors();
		Position p = new Position(getX(),getY());
		
		c.bodyColor = Color.black;
		c.gunColor = Color.white;
		c.radarColor = Color.orange;
		c.scanColor = Color.yellow;
		c.bulletColor = Color.blue;

		// Set the color of this robot containing the RobotColors
		setBodyColor(c.bodyColor);
		setGunColor(c.gunColor);
		setRadarColor(c.radarColor);
		setScanColor(c.scanColor);
		setBulletColor(c.bulletColor);
		try {
			// Send RobotColors object to our entire team
			broadcastMessage(c);
			broadcastMessage(p);
		} catch (IOException ignored) {}
		// Normal behavior
		while (true) {
			setTurnRadarRight(10000);
			tactics();
			enemies.clear();
			rv++;
		}
	}
	
	public void onStatus(StatusEvent e){
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}
	
	/**
	 * onHitByBullet:  Turn perpendicular to bullet path
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		
	
		// Set the target
		target = e.getName();
		double gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		Help help = new Help(e.getName(), new Position(getX(), getY()), priority(getEnergy()),gunTurnAmt);
		
		if(getEnergy()>50){
			try {
				// mudar para a defesa pessoal ....
				sendMessage("teamRobots.Cabo",help);
				sendMessage("teamRobots.Soldado",help);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
		
			turnGunRight(gunTurnAmt);
			fire(2.5);
		}
		// Back up a bit.
		// Note:  We won't get scan events while we're doing this!
		// An AdvancedRobot might use setBack(); execute();
		
		turnGunRight(gunTurnAmt);
		fire(1);

	}
	
	public int priority(double energy){
		if (energy > 20) {
			return 5;
		} else if (energy > 40) {
			return 4;
		} else if (energy > 60) {
			return 3;
		} else if (energy > 80) {
			return 2;
		} else if (energy > 100) {
			return 1;
		}
	return 0;
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
		out.println("rv :"+rv+"          "+tg.getPower());
		if(tg.getPower()>=80 && rv<5){
			try {
				out.println("aqui vai o teu target");
				broadcastMessage(tg);
				//sendMessage("teamRobots.Atirador",tg);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			rival=tg;
			rv=5;
		}
		
		Attack at = new Attack(priority(getEnergy()), target());
		
		try {
			// mudar para a defesa pessoal ....
			broadcastMessage(at);
			broadcastMessage(new Shot(e));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		
		double dt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(dt);
		fire(0.5);
		scan();	
	}

	public void team(){
		String[] s = getTeammates();
		team=(ArrayList<String>) Arrays.asList(s);
	}

	public Target target(){
	Move m = null;
	double distance=0;
	double d=0;
	double power=10000;
	Target tg=null;
		for(Target t : enemies){
			m = new Move(new Position(getX(),getY()),new Position(t.getP().getX(),t.getP().getX()));
			distance = m.move();
			if(t.getPower()<power){
			//if(distance>d){
			power=t.getPower();	
			//d=distance;
				tg=t;
			}
		}
	return tg;
	}
	
	public void tactics(){
		if(enemies.size()>getTeammates().length) {
			out.println("defender");
			tactics="defense";
			goCorner();	
			Position p = new Position(getX(),getY());
			try {
				broadcastMessage(p);
			} catch (IOException ignored) {}
		}
		if(enemies.size()<getTeammates().length ) {
			tactics="attack";
			
			Position p1 = nearest();
			out.println("atacar");
			
			double dx = p1.getX() - this.getX();
			double dy = p1.getY() - this.getY();
			// Calculate angle to target
			double theta = Math.toDegrees(Math.atan2(dx, dy));

			
			Move move= new Move(new Position(getX(),getY()),new Position(p1.getX(),p1.getY()));
			
			
			turnRight(normalRelativeAngleDegrees(theta - getGunHeading()));
			ahead(move.move()-10);
			try {
				out.println("aqui vai o teu target");
				broadcastMessage(p1);
				//sendMessage("teamRobots.Atirador",tg);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			
			
		}
	}
	
	public void goCorner() {
        // vira o robot para a parede � direita do canto pretendido
        turnRight(Utils.normalRelativeAngleDegrees(corner - getHeading()));
        // Movimentos para se aproximar do canto
        if (corner == 0){
	        ahead(getBattleFieldHeight()-getY()-20);
	        turnLeft(90);
	        ahead(getX()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()) + 90);
        }
        else if (corner == 90){
        	ahead(getBattleFieldWidth()-getX()-20);
	        turnLeft(90);
	        ahead(getBattleFieldHeight()-getY()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()) - 90);
        }
        else if (corner == 180){
        	ahead(getY()-20);
	        turnLeft(90);
	        ahead(getBattleFieldWidth()-getX()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()));
        }
        else if (corner == 270){
        	ahead(getX()-20);
	        turnLeft(90);
	        ahead(getY()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()));
        }
	}
	
	public Position nearest(){
		Move m = null;
		double distance=0;
		double d=1000000;
		Position pt=new Position(500, 500);
			for(Target t : enemies){
				m = new Move(new Position(getX(),getY()),new Position(t.getP().getX(),t.getP().getX()));
				distance = m.move();
				if(distance<d){
					d=distance;
					pt=t.getP();
				}
			}
		return pt;
		
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
