package cfp;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;
import java.awt.geom.Point2D;

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
import robocode.util.Utils;

public class Petulancia extends AdvancedRobot { //Deliberativa - nao foge kd leva tiro e dispara usando um algoritmo preditivo circular

	private int count;
	private String trackName;
	private double gunTurnAmt;
	private double energyAdv;

	private double openness = 0.22;
	private double conscientiousness = 0.34;
	private double extraversion = 0.73;
	private double agreeableness = 0.10;
	private double neuroticism = 0.15;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
	
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	public void run(){
		// Define as cores do robot
		setBodyColor(Color.black);
		setGunColor(Color.black);
		setRadarColor(Color.black);
		setScanColor(Color.black);
		setBulletColor(Color.black);
		
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

		//Variaveis usadas no uso do calculo da posicao futura do robot detetado
		double currentX = getX();
		double currentY = getY();
		double bearingTotal = getHeadingRadians() + e.getBearingRadians();
		double enemyCurrentX = getX() + e.getDistance() * Math.sin(bearingTotal);
		double enemyCurrentY = getY() + e.getDistance() * Math.cos(bearingTotal);
		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyCurrentX, predictedY = enemyCurrentY;
		
		count = 0;
		while((++deltaTime) * 11 < Point2D.Double.distance(currentX, currentY, predictedX, predictedY)){		
			predictedX += Math.sin(enemyHeading) * enemyVelocity;	
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			if(	predictedX < 18.0 || predictedY < 18.0 || predictedX > battleFieldWidth - 18.0 || predictedY > battleFieldHeight - 18.0){
				
				predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);	
				predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
		 
		setTurnRadarRightRadians(Utils.normalRelativeAngle(bearingTotal - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
		fire(3);

		emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		
		//Apos disparar sobre o robot detetado, aproxima-se ate 300 unidades perto dele
		if (e.getDistance() > 500) {
			gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
			turnGunRight(gunTurnAmt);
			turnRight(e.getBearing());
			ahead(e.getDistance() - 300);
			return;
		}
		energyAdv = e.getEnergy();
		scan();
	}
	
	/**
	 * onHitRobot: verifica se o robot tem mais energia que o seu alvo e, se tiver, torna-o o seu alvo e dispara sobre ele
	 * 			   caso contrario, ignora o robot e recua um quarto da altura do terreno
	 */
	public void onHitRobot(HitRobotEvent e) {

		if(e.getEnergy() > energyAdv) {
			trackName = e.getName();
			gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
			turnGunRight(gunTurnAmt);
			fire(3);
		}
		else back(getBattleFieldHeight()/4);

		emocaoActiva = new Point3D(Point3D.hate.getX(),Point3D.hate.getY(),Point3D.hate.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	/**
	 * onHitWall: muda a direcao e desloca-se em direcao ao centro do terreno
	 */
	public void onHitWall(HitWallEvent e){
		gunTurnAmt = getHeading()-180;
		turnRight(gunTurnAmt);
		ahead(getBattleFieldHeight()/4);
		
		emocaoActiva = new Point3D(Point3D.remorse.getX(),Point3D.remorse.getY(),Point3D.remorse.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	/**
	 * onHitByBullet: roda a arma em direcao ˆ posicao do robot que o atingiu e dispara um tiro. 
	 * 				  nao se importa com o robot, isto Ž, nao o torna o seu alvo
	 */
	public void onHitByBullet(HitByBulletEvent e) {

		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(gunTurnAmt);
		fire(3);

		emocaoActiva = new Point3D(Point3D.anger.getX(),Point3D.anger.getY(),Point3D.anger.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){

		emocaoActiva = new Point3D(Point3D.disappointment.getX(),Point3D.disappointment.getY(),Point3D.disappointment.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onBulletHit(BulletHitEvent e){

		emocaoActiva = new Point3D(Point3D.pride.getX(),Point3D.pride.getY(),Point3D.pride.getZ());
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
