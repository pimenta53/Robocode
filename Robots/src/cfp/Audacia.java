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

public class Audacia extends AdvancedRobot { //Hibrida - vai agir como audaz mas kd tem vida < 50, vai desviar-se dos tiros
											// e disparar usando um algoritmo preditivo linear

	private int count;
	private String trackName;
	private double gunTurnAmt;
	private double energyAdv;
	private boolean intBehaviour = false;

	private double openness = 0.78;
	private double conscientiousness = 0.90;
	private double extraversion = 0.50;
	private double agreeableness = 0.88;
	private double neuroticism = 0.35;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
	
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	//variáveis para se desviar das balas
	private double previousEnergy = 100;
	private int movementDirection = 1;
	
	//variável que guarda os dados sobre o robot que se pretende prever o trajecto
	private AdvancedEnemyBot enemy = new AdvancedEnemyBot();
	
	public void run(){
		// Define as cores do robot
		setBodyColor(Color.green);
		setGunColor(Color.green);
		setRadarColor(Color.green);
		setScanColor(Color.green);
		setBulletColor(Color.green);
		
		count = 0;
		energyAdv = 0;
		trackName = null;
		setAdjustGunForRobotTurn(true);
		gunTurnAmt = 10;
		
		while (true) {
			if (!intBehaviour){
				turnGunRight(gunTurnAmt);
				count++;
				if (count > 1) gunTurnAmt = -10;
				if (count > 2) gunTurnAmt = 10;
				//se ao fim de 3 turnos, ainda nao encontrou o alvo, abandona esse alvo
				if (count > 3) trackName = null;
			} else turnGunRight(360);
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
		if (!intBehaviour){ //Comportamento muito audacioso
			
			//se o robot detetado se encontra a mais de 500 unidades, vira a arma para ele e dispara um tiro com 3 (audácia)
			if (e.getDistance() > 500) {
				gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
				turnGunRight(gunTurnAmt);
				turnRight(e.getBearing());
				ahead(e.getDistance() - 400);
				fire(3);
				emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
				double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
				if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
				return;
			}
			
			gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
			turnGunRight(gunTurnAmt);
	
			if (e.getEnergy() > 16) {fireBullet(3);} 
			else if (e.getEnergy() > 10) {fireBullet(2);} 
			else if (e.getEnergy() > 4) {fire(1);} 
			else if (e.getEnergy() > 2) {fire(.5);}
			else if (e.getEnergy() > .4) {fire(.1);}
			
			emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
			turnRight(e.getBearing());
			ahead(5);
		}
		else { //Comportamento mais cauteloso
			
			setTurnRight(e.getBearing() + 90 - 30*movementDirection);
			// Se o robot tem uma pequena descida de vida, assume que ele disparou
			double changeInEnergy = previousEnergy - e.getEnergy();
			if (changeInEnergy > 0 && changeInEnergy <= 3) {
				// Esquiva-se
				movementDirection = -movementDirection;
				setAhead((e.getDistance() / 4 + 25) * movementDirection);
			}
			
			// Calcula potencia do tiro com base na distancia do inimigo
			double firePower = Math.min(500 / enemy.getDistance(), 3);
			double bulletSpeed = 20 - firePower * 3;
			long time = (long)(enemy.getDistance() / bulletSpeed);
			
			//verifica se nao temos nenhum inimigo, se o que encontramos se encontra perto ou se detetamos o robot que procuravamos
			if (enemy.none() || e.getDistance() < enemy.getDistance() - 70 || e.getName().equals(enemy.getName())) {
				enemy.update(e, this);
			}
			
			double futureX = enemy.getFutureX(time);
			double futureY = enemy.getFutureY(time);
			double absDeg = absoluteBearing(getX(), getY(), futureX, futureY);
			setTurnGunRight(normalizeBearing(absDeg - getGunHeading()));
			fire(firePower);

			emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
			previousEnergy = e.getEnergy();
		}
		energyAdv = e.getEnergy();
		scan();
	}
	
	/**
	 * onBulletHit: se ficou com mais de 50 de vida, volta a adoptar uma tactica mais audaz
	 */
	public void onBulletHit(BulletHitEvent e){
		if (getEnergy() > 50) intBehaviour = false;

		emocaoActiva = new Point3D(Point3D.pride.getX(),Point3D.pride.getY(),Point3D.pride.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	/**
	 * onHitRobot: se ficou com menos de 50 de vida, adopta um comportamento mais cauteloso
	 */
	public void onBulletMissed(BulletMissedEvent e){
		if (getEnergy() < 50) intBehaviour = true;

		emocaoActiva = new Point3D(Point3D.distress.getX(),Point3D.distress.getY(),Point3D.distress.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}

	/**
	 * onHitRobot: torna o robot o seu alvo e dispara um tiro
	 */
	public void onHitRobot(HitRobotEvent e) {
		trackName = e.getName();
		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		turnGunRight(gunTurnAmt);
		fire(3);

		emocaoActiva = new Point3D(Point3D.anger.getX(),Point3D.anger.getY(),Point3D.anger.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		energyAdv = e.getEnergy();
	}
	
	/**
	 * onHitWall: se ficar com menos de 50 de vida, passa a ter um comportamento mais cauteloso
	 * 			  muda de direcçao para o centro do terreno
	 */
	public void onHitWall(HitWallEvent e){
		gunTurnAmt = getHeading()-180;
		turnRight(gunTurnAmt);
		ahead(getBattleFieldWidth()/4);
		if (getEnergy() < 50) intBehaviour = true;

		emocaoActiva = new Point3D(Point3D.remorse.getX(),Point3D.remorse.getY(),Point3D.remorse.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	/**
	 * onHitByBullet: se ficar com menos de 50 de vida, passa a ter um comportamento mais inteligente
	 * 				  comportamento mais audaz: dispara um tiro na direcçao do robot inimigo
	 * 				  comportamento mais cauteloso: muda a sua posiçao
	 */
	public void onHitByBullet(HitByBulletEvent e) {

		if (getEnergy() < 50) intBehaviour = true;
		gunTurnAmt = normalRelativeAngleDegrees(e.getBearing() + (getHeading() - getRadarHeading()));
		if(!intBehaviour) {
			turnGunRight(gunTurnAmt);
			fire(3);
		}
		else {
			gunTurnAmt -= 135;
			turnRight(gunTurnAmt);
			ahead(getBattleFieldWidth()/4);
		}

		emocaoActiva = new Point3D(Point3D.anger.getX(),Point3D.anger.getY(),Point3D.anger.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e){

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
	
	// calcula o bearing absoluto entre dois pontos
	double absoluteBearing(double x1, double y1, double x2, double y2) {
		double xo = x2-x1;
		double yo = y2-y1;
		double hyp = Point2D.distance(x1, y1, x2, y2);
		double arcSin = Math.toDegrees(Math.asin(xo / hyp));
		double bearing = 0;

		// ambos positivos: inferior esquerdo
		if (xo > 0 && yo > 0) {bearing = arcSin;} 
		// x neg, y pos: inferior direito
		else if (xo < 0 && yo > 0) {bearing = 360 + arcSin;} // arcsin é negativo aqui, na realidade: 360 - ang 
		// x pos, y neg: superior esquerdo
		else if (xo > 0 && yo < 0) {bearing = 180 - arcSin;}
		// ambos negativos: superior direito
		else if (xo < 0 && yo < 0) {bearing = 180 - arcSin;} // arcsin é negativo aqui, na realidade: 180 + ang

		return bearing;
	}
	
	// normaliza o bearing para valores entre -180 e +180
	double normalizeBearing(double angle) {
		while (angle >  180) angle -= 360;
		while (angle < -180) angle += 360;
		return angle;
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
