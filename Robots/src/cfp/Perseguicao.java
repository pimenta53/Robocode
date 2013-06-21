package cfp;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;

import ontology.Point3D;

import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.Robot;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

public class Perseguicao extends Robot{

	private String target;
	private double dt=45;
	
	private int times;
	private double height;
	private double width;

	private double openness = 0.45;
	private double conscientiousness = 0.20;
	private double extraversion = 0.64;
	private double agreeableness = 0.89;
	private double neuroticism = 0.33;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
		
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	public void run(){
		
		// detectar dimensões do campo de batalha
		height= getHeight();
		width= getWidth();
		
		// Prepare gun
				target = null; // Initialize to not tracking anyone
				setAdjustGunForRobotTurn(true); // Keep the gun still when we turn
				dt = 10; // Initialize gunTurn to 10
		
		
		while (true) {
	
			// turn the Gun (looks for enemy)
			turnGunRight(dt);
			// Keep track of how long we've been looking
			times++;
				
			// If we've haven't seen our target for 2 turns, look left
			if (times > 2) {
				setBodyColor(Color.orange);
				dt = -10;
			}
			// If we still haven't seen our target for 5 turns, look right
			if (times > 5) {
				setBodyColor(Color.magenta);
				dt = 10;
			}
			// If we *still* haven't seen our target after 10 turns, find another target
			if (times > 100 ) {
				target = null;
				setBodyColor(Color.CYAN);
			}
			if (emocaoActiva != null){
				double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
				if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva, intense, 
																				((int) System.currentTimeMillis()/100)-t);
			}
		}		
	}
	
	public void onStatus(StatusEvent e){
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}
	
	
	public void onScannedRobot(ScannedRobotEvent e) {
	
		// se não é o alvo sair para procurar logo outro ...		
		if (target != null && !e.getName().equals(target)) {
			return;
		}
		
		// se não tem target, escolhe um que seja mais fraco do que ele para seguir!!!
		if (target == null) {
					if(e.getEnergy()<this.getEnergy()) target = e.getName();
					out.println("O meu alvo é este:" + target);
		}
			
		// está a ver o robot que lhe interessa ....
		times = 0;
		
		// se for mais fraco vai atrás dele com toda a força e pojança
		if (e.getDistance() > 150) {
			dt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));

			setBodyColor(Color.yellow);
			//setTurnGunRight(dt); // virar a arma e radar em direcção ao alvo ...
			turnRight(dt);
			turnRight(e.getBearing()); // and see how much Tracker improves...
			// (you'll have to make Tracker an AdvancedRobot)
			if(getEnergy()>e.getEnergy()) ahead(e.getDistance() - 100);
			else ahead(e.getDistance() - 400);
			
			return;
		}
		
		setBodyColor(Color.green);
		dt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(dt);
		if ( e.getEnergy()<getEnergy()) {
			fireC(e.getEnergy());
			emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		}
		
			//quando está perto dele!! afasta-se um pouco para não o perder de vista
		if(e.getDistance()<100){		
			setBodyColor(Color.red);
			if (e.getBearing() > -90 && e.getBearing() <= 90) back(40);
			else ahead(40);
			
		}
		
		scan();			
	}

	// quando leva um tiro
		public void onHitByBullet(HitByBulletEvent e) {
			// Only print if he's not already our target.
			if (target != null && target.equals(e.getName())) {
				dt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
				turnGunRight(dt);
				fire(1);
			}
			else ;//desmarcar ...

			emocaoActiva = new Point3D(Point3D.anger.getX(),Point3D.anger.getY(),Point3D.anger.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		}
		
		public void fireC(double energi){
		
		if (energi > 16) {
			fire(3);
		} else if (energi > 10) {
			fire(2);
		} else if (energi > 4) {
			fire(1);
		} else if (energi > 2) {
			fire(.5);
		} else if (energi > .4) {
			fire(.1);
		}
	}

	public void onHitRobot(HitRobotEvent e) {
		
		emocaoActiva = new Point3D(Point3D.joy.getX(),Point3D.joy.getY(),Point3D.joy.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}

	public void onHitWall(HitWallEvent e){
		
		emocaoActiva = new Point3D(Point3D.remorse.getX(),Point3D.remorse.getY(),Point3D.remorse.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){

		emocaoActiva = new Point3D(Point3D.disappointment.getX(),Point3D.disappointment.getY(),Point3D.disappointment.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onBulletHit(BulletHitEvent e){

		emocaoActiva = new Point3D(Point3D.joy.getX(),Point3D.joy.getY(),Point3D.joy.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onBulletMissed(BulletMissedEvent e){

		emocaoActiva = new Point3D(Point3D.disappointment.getX(),Point3D.disappointment.getY(),Point3D.disappointment.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onDeath(){
		emocaoActiva = new Point3D(Point3D.hate.getX(),Point3D.hate.getY(),Point3D.hate.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onWin(){
		emocaoActiva = new Point3D(Point3D.pride.getX(),Point3D.pride.getY(),Point3D.pride.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
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
