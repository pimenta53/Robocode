package cfp;

import static robocode.util.Utils.normalRelativeAngleDegrees;
import ontology.Point3D;
import robocode.*;

public class Fugitivo extends AdvancedRobot{
	
	private int moveDirection = 1; // Clockwise or counterclockwise
	private int view=0;

	private double openness = 0.68;
	private double conscientiousness = 0.70;
	private double extraversion = 0.59;
	private double agreeableness = 0.25;
	private double neuroticism = 0.40;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
		
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	public void run(){
		
		while (true) {
	        // sempre a tentar ver o que se passa para fugir ....
			turnGunLeft(360);
			if (emocaoActiva != null){
				double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
				if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva, intense, 
																			((int) System.currentTimeMillis()/100)-t);
			}
			execute();
		}
	}
	
	public void onStatus(StatusEvent e){
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		
		if(getEnergy()>30){
		// se vÊ alguém mais forte do que ele ....
		view++;
    	// switch directions if we've stopped
    	if (getVelocity() == 0)
    		moveDirection *= -1;
    		// fugir aos esses
    		if (view % 2 == 0) setTurnRight(-e.getBearing() + 90);
    		else setTurnLeft(-e.getBearing() + 90);
    		setAhead(500 * moveDirection);
		}
		else{
			// switch directions if we've stopped
    		if (getVelocity() == 0)
    			moveDirection *= -1;

    		setTurnRight(e.getBearing() + 90);
    		setAhead(1000 * moveDirection);
    	
    		double dt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));

			turnGunRight(dt); // Try changing these to setTurnGunRight,
    		fire(3);

    		emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
    		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
    		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		}
    }

	// quando leva um tiro vinga-se ....
	public void onHitByBullet(HitByBulletEvent e) {
		// Only print if he's not already our target.
		double	dt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(dt);
		fire(1);				

		emocaoActiva = new Point3D(Point3D.distress.getX(),Point3D.distress.getY(),Point3D.distress.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}

	public void onHitRobot(HitRobotEvent e) {
			
		emocaoActiva = new Point3D(Point3D.distress.getX(),Point3D.distress.getY(),Point3D.distress.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
		
	public void onHitWall(HitWallEvent e){
		emocaoActiva = new Point3D(Point3D.fear.getX(),Point3D.fear.getY(),Point3D.fear.getZ());
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
		emocaoActiva = new Point3D(Point3D.relief.getX(),Point3D.relief.getY(),Point3D.relief.getZ());
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
