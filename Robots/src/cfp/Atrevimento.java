package cfp;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import ontology.Point3D;

import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

public class Atrevimento extends AdvancedRobot { //Deliberativa - ataca o que tem mais vida e foge kd leva um tiro com mais de 1.5

	private int count;
	private String trackName;
	private double gunTurnAmt;
	private double energyAdv;

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
		// Define as cores do robot
		setBodyColor(Color.red);
		setGunColor(Color.red);
		setRadarColor(Color.red);
		setScanColor(Color.red);
		setBulletColor(Color.red);
		
		count = 0;
		energyAdv = 0;
		trackName = null;
		setAdjustGunForRobotTurn(true);
		gunTurnAmt = 10;
		
		while (true) {
			turnGunRight(gunTurnAmt);
			count++;
			if (count > 1) gunTurnAmt = -10;
			if (count > 2) gunTurnAmt = 10;
			//se ao fim de 3 turnos, ainda nao encontrou o alvo, abandona esse alvo
			if (count > 3) trackName = null;
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
		// Caso nao seja o alvo atual, verifica se este possui mais energia e, em caso afirmativo, torna-o o seu alvo atual
		
		if (trackName != null && !e.getName().equals(trackName)) {
			if (e.getEnergy() > energyAdv) trackName = e.getName();
			else return;
		}

		// Torna o robot detetado o seu alvo atual caso ainda nao possua um
		if (trackName == null) {
			trackName = e.getName();
			energyAdv = e.getEnergy();
		}

		count = 0;
		
		// Persegue o robot inimigo se este estiver a uma distancia maior do que 250
		if (e.getDistance() > 250) {
			gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));

			turnGunRight(gunTurnAmt);
			turnRight(e.getBearing());
			ahead(e.getDistance() - 100);
			return;
		}

		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(gunTurnAmt);
		fire(3);
		emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);

		// Se o robot inimigo estiver a uma distancia menor de 150, dispara um tiro consoante a energia do robot inimigo
		if (e.getDistance() <= 150) {
			if (e.getEnergy() > 16) {fireBullet(3);} 
			else if (e.getEnergy() > 10) {fireBullet(2);} 
			else if (e.getEnergy() > 4) {fire(1);} 
			else if (e.getEnergy() > 2) {fire(.5);}
			else if (e.getEnergy() > .4) {fire(.1);}
			turnRight(e.getBearing());
			ahead(5);	
			emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
			intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		}
		energyAdv = e.getEnergy();
		scan();
	}

	/**
	 * onHitRobot: verifica se o robot tem mais energia que o seu alvo e, se tiver, torna-o o seu alvo
	 * 			   de qualquer forma, roda a arma para a posi�‹o do robot e dispara
	 */
	public void onHitRobot(HitRobotEvent e) {
		
		if(e.getEnergy() > energyAdv) trackName = e.getName();
		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(gunTurnAmt);
		fire(3);
		emocaoActiva = new Point3D(Point3D.pride.getX(),Point3D.pride.getY(),Point3D.pride.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	/**
	 * onHitWall: muda a direcao atual e desloca-se um pouco para o centro do terreno
	 */
	public void onHitWall(HitWallEvent e){
		gunTurnAmt = getHeading()-180;
		turnRight(gunTurnAmt);
		ahead(getBattleFieldWidth()/4);
		emocaoActiva = new Point3D(Point3D.remorse.getX(),Point3D.remorse.getY(),Point3D.remorse.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	/**
	 * onHitByBullet: Se a bala tiver mais de 1.5 de poder, guarda quem o atingiu, dispara sobre ele e desvia-se para evitar futuras
	 * 				  balas
	 */ 
	public void onHitByBullet(HitByBulletEvent e) {
		if (e.getBullet().getPower() > 1.5) {
			trackName = e.getName();
			gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
			turnGunRight(gunTurnAmt);
			fire(3);
			gunTurnAmt -= 90;
			turnRight(gunTurnAmt);
			ahead(getBattleFieldWidth()/4);
		}
		emocaoActiva = new Point3D(Point3D.hate.getX(),Point3D.hate.getY(),Point3D.hate.getZ());
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
		emocaoActiva = new Point3D(Point3D.anger.getX(),Point3D.anger.getY(),Point3D.anger.getZ());
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
