package cfp;

import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.awt.Color;

import ontology.Point3D;
import robocode.*;

/* 
 * s.f. Ato de apreender; tomada, prisão. Receio, cisma, preocupação: 
 * ter apreensão diante do desconhecido. Compreensão, conhecimento: 
 * a apreensão das noções de espaço e tempo. (reactiva)
 */
public class Apreensao extends AdvancedRobot { // Reativo

	// Direção do robô
	private int direcao = 1;

	// Distancia a percorrer
	private double distancia = 100;

	// PI*2
	private static final double DOUBLE_PI = (Math.PI * 2);

	// PI/2
	private static final double HALF_PI = (Math.PI / 2);

	// Variáveis que controlam o quanto junto da parede o robô pode estar
	private static final double WALL_AVOID_INTERVAL = 10;
	private static final double WALL_AVOID_FACTORS = 20;
	private static final double WALL_AVOID_DISTANCE = (WALL_AVOID_INTERVAL * WALL_AVOID_FACTORS);

	// Variáveis para o modelo OCC
	private double openness = 0.95;
	private double conscientiousness = 0.80;
	private double extraversion = 0.20;
	private double agreeableness = 0.25;
	private double neuroticism = 0.60;
	
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21
			* extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15
			* openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25
			* openness;
	private int t;
	private Point3D emocaoActiva;
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;

	public void run() {

		setAdjustGunForRobotTurn(true);
		
		// Cores
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.blue);
		setScanColor(Color.blue);
		setBulletColor(Color.blue);

		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		while (true) {
			turnRadarLeftRadians(1);

			// Enquanto poder vai ter tentar andar em frente evitando as paredes
			setTurnRightRadiansOptimal(adjustHeadingForWalls(1));
			setAhead(distancia);
			execute();

			if (emocaoActiva != null) {
				double intense = getEmotionIntensity(emocaoActiva.getX(),
						emocaoActiva.getY(), emocaoActiva.getZ());
				if (intense > Math.abs(neuroticism - extraversion))
					updateEmotion(emocaoActiva, intense,
							((int) System.currentTimeMillis() / 100) - t);
			}
		}
	}
	
	public void onStatus(StatusEvent e){
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}
	
	// Ao bater numa parede
	public void onHitWall(HitWallEvent e) {
		emocaoActiva = new Point3D(Point3D.remorse.getX(),
				Point3D.remorse.getY(), Point3D.remorse.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
		
		reverterPosicao();
	}

	// Ao bater noutro robô
	public void onHitRobot(HitRobotEvent roboAdv) {
		
		emocaoActiva = new Point3D(Point3D.distress.getX(),
				Point3D.distress.getY(), Point3D.distress.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
		
		
		double x = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(x);

		fire(3);

		reverterPosicao();
	}

	// Ao detetar um robô
	public void onScannedRobot(ScannedRobotEvent roboAdv) {
		emocaoActiva = new Point3D(Point3D.anger.getX(),
				Point3D.anger.getY(), Point3D.anger.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
		
		
		double x = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));

		// Roda a arma em direção ao robo
		turnGunRight(x);

		// Determina o poder de disaparo
		double poderDisparo = Math.min(500 / roboAdv.getDistance(), 3);
		fire(poderDisparo);

		// Reverte a sua posição e foge
		reverterPosicao();
	}
	
	public void onBulletHitBullet(BulletHitBulletEvent e) {

		emocaoActiva = new Point3D(Point3D.disappointment.getX(),
				Point3D.disappointment.getY(), Point3D.disappointment.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onBulletHit(BulletHitEvent e) {

		emocaoActiva = new Point3D(Point3D.gratitude.getX(), Point3D.gratitude.getY(),
				Point3D.gratitude.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onBulletMissed(BulletMissedEvent e) {

		emocaoActiva = new Point3D(Point3D.hate.getX(), Point3D.hate.getY(),
				Point3D.hate.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onDeath() {
		emocaoActiva = new Point3D(Point3D.disappointment.getX(),
				Point3D.disappointment.getY(), Point3D.disappointment.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onWin() {
		emocaoActiva = new Point3D(Point3D.joy.getX(),
				Point3D.joy.getY(), Point3D.joy.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	// Reverter a posição do robô
	public void reverterPosicao() {

		double d = (getDistanceRemaining() * direcao);
		direcao *= -1;
		setAhead(d);

	}

	// Metodos de ajuda
	private double adjustHeadingForWalls(double heading) {

		double fieldHeight = getBattleFieldHeight();
		double fieldWidth = getBattleFieldWidth();
		double centerX = (fieldWidth / 2);
		double centerY = (fieldHeight / 2);

		double currentHeading = getRelativeHeadingRadians();
		double x = getX();
		double y = getY();

		boolean nearWall = false;

		double desiredX;
		double desiredY;

		if ((y < WALL_AVOID_DISTANCE)
				|| ((fieldHeight - y) < WALL_AVOID_DISTANCE)) {
			desiredY = centerY;
			nearWall = true;
		} else {
			desiredY = y;
		}
		if ((x < WALL_AVOID_DISTANCE)
				|| ((fieldWidth - x) < WALL_AVOID_DISTANCE)) {
			desiredX = centerX;
			nearWall = true;
		} else {
			desiredX = x;
		}
		if (nearWall) {
			double desiredBearing = calculateBearingToXYRadians(x, y,
					currentHeading, desiredX, desiredY);
			double distanceToWall = Math.min(Math.min(x, (fieldWidth - x)),
					Math.min(y, (fieldHeight - y)));
			int wallFactor = (int) Math.min(
					(distanceToWall / WALL_AVOID_INTERVAL), WALL_AVOID_FACTORS);
			return ((((WALL_AVOID_FACTORS - wallFactor) * desiredBearing) + (wallFactor * heading)) / WALL_AVOID_FACTORS);
		} else {
			return heading;
		}
	}

	public double getRelativeHeadingRadians() {
		double relativeHeading = getHeadingRadians();
		if (direcao < 1) {
			relativeHeading = normalizeAbsoluteAngleRadians(relativeHeading
					+ Math.PI);
		}
		return relativeHeading;
	}

	public void alterarDirecao() {
		double distance = (getDistanceRemaining() * direcao);
		direcao *= -1;
		setAhead(distance);
	}

	public void setAhead(double distance) {
		double relativeDistance = (distance * direcao);
		super.setAhead(relativeDistance);
		// If distance is negative, reverse our direcao
		if (distance < 0) {
			direcao *= -1;
		}
	}

	public void setBack(double distance) {
		double relativeDistance = (distance * direcao);
		super.setBack(relativeDistance);
		// If distance is positive, reverse our direcao
		if (distance > 0) {
			direcao *= -1;
		}
	}

	public void setTurnLeftRadiansOptimal(double angle) {
		double turn = normalizeRelativeAngleRadians(angle);
		if (Math.abs(turn) > HALF_PI) {
			alterarDirecao();
			if (turn < 0) {
				turn = (HALF_PI + (turn % HALF_PI));
			} else if (turn > 0) {
				turn = -(HALF_PI - (turn % HALF_PI));
			}
		}
		setTurnLeftRadians(turn);
	}

	public void setTurnRightRadiansOptimal(double angle) {
		double turn = normalizeRelativeAngleRadians(angle);
		if (Math.abs(turn) > HALF_PI) {
			alterarDirecao();
			if (turn < 0) {
				turn = (HALF_PI + (turn % HALF_PI));
			} else if (turn > 0) {
				turn = -(HALF_PI - (turn % HALF_PI));
			}
		}
		setTurnRightRadians(turn);
	}

	public double calculateBearingToXYRadians(double sourceX, double sourceY,
			double sourceHeading, double targetX, double targetY) {
		return normalizeRelativeAngleRadians(Math.atan2((targetX - sourceX),
				(targetY - sourceY)) - sourceHeading);
	}

	public double normalizeAbsoluteAngleRadians(double angle) {
		if (angle < 0) {
			return (DOUBLE_PI + (angle % DOUBLE_PI));
		} else {
			return (angle % DOUBLE_PI);
		}
	}

	public static double normalizeRelativeAngleRadians(double angle) {
		double trimmedAngle = (angle % DOUBLE_PI);
		if (trimmedAngle > Math.PI) {
			return -(Math.PI - (trimmedAngle % Math.PI));
		} else if (trimmedAngle < -Math.PI) {
			return (Math.PI + (trimmedAngle % Math.PI));
		} else {
			return trimmedAngle;
		}
	}

	// Métodos Adicionais do Modelo OCC
	private double getEmotionIntensity(double x, double y, double z) {
		return Math.sqrt(x * x + y * y + z * z) / Math.sqrt(3);
	}

	private void updateEmotion(Point3D newEmotion, double intensity, int tempo) {

		Pcurrent = Pbase + (newEmotion.getX() * emotionI(tempo, intensity));
		Acurrent = Abase + (newEmotion.getY() * emotionI(tempo, intensity));
		Dcurrent = Dbase + (newEmotion.getZ() * emotionI(tempo, intensity));
		if (tempo == 0)
			t = (int) System.currentTimeMillis() / 100;
	}

	private double emotionI(int t, double intensity) {
		return intensity * Math.exp(-t * neuroticism);
	}
}
