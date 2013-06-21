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
import robocode.TurnCompleteCondition;

/*Sobressalto, terror repentino. Medo, receio.*/
public class Susto extends AdvancedRobot { // Reactiva: apenas reage a a
											// determinadas situações

	// Direcção do robô
	private int direcao = 1;

	// Distancia a percorrer
	private double distancia = 100;

	// Risco de vida (energia < 30%)
	private boolean riscoVida = false;

	// Variáveis para o modelo OCC
	private double openness = 0.20;
	private double conscientiousness = 0.45;
	private double extraversion = 0.10;
	private double agreeableness = 0.10;
	private double neuroticism = 0.95;

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
		setBodyColor(Color.white);
		setGunColor(Color.black);
		setRadarColor(Color.white);
		setScanColor(Color.black);
		setBulletColor(Color.black);

		// Ajusta o radar e a arma
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Enquanto for verdade
		while (true) {

			// Numero de movimentos em frente no campo de batalha
			double numeroMovimentos = Math.max(getBattleFieldWidth() / 2,
					getBattleFieldHeight() / 2);
			turnLeft(getHeading());
			ahead(numeroMovimentos);
			turnLeft(90);

			double intense;

			// Se tiver em risco de vida (energia < 30%)
			if (riscoVida) {
				// Fica muito assustado
				emocaoActiva = new Point3D(Point3D.fear.getX(),
						Point3D.fear.getY(), Point3D.fear.getZ());
				intense = getEmotionIntensity(emocaoActiva.getX(),
						emocaoActiva.getY(), emocaoActiva.getZ());

				muitoAssustado();
			} else {
				// Senão continua a andar para a frente
				emocaoActiva = new Point3D(Point3D.distress.getX(),
						Point3D.distress.getY(), Point3D.distress.getZ());
				intense = getEmotionIntensity(emocaoActiva.getX(),
						emocaoActiva.getY(), emocaoActiva.getZ());
			}

			if (intense > Math.abs(neuroticism - extraversion))
				updateEmotion(emocaoActiva, intense, 0);
			execute();

			if (emocaoActiva != null) {
				intense = getEmotionIntensity(emocaoActiva.getX(),
						emocaoActiva.getY(), emocaoActiva.getZ());
				if (intense > Math.abs(neuroticism - extraversion))
					updateEmotion(emocaoActiva, intense,
							((int) System.currentTimeMillis() / 100) - t);
			}
		}
	}

	// Ao detetar outro robô
	public void onScannedRobot(ScannedRobotEvent roboAdv) {

		// Ajusta o radar e a arma
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Distancia do robô
		double d = roboAdv.getDistance();

		// Vira-se para o adversário que está a x angulos dele
		double x = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(x);

		// Determinar o força de disparo apropriada
		fire(forcaDisparo(d));

		emocaoActiva = new Point3D(Point3D.fear.getX(), Point3D.fear.getY(),
				Point3D.fear.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);

		// Foge
		fugir();
	}

	// Se for antingido por uma bala: assusta-se, dispara e foge
	public void onHitByBullet(HitByBulletEvent e) {

		// Ajusta o radar e a arma
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Vira-se para o adversário que está a x angulos dele
		double x = normalRelativeAngleDegrees(e.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(x);

		// E dispara com a máxima potência
		fire(3);

		emocaoActiva = new Point3D(Point3D.disappointment.getX(),
				Point3D.disappointment.getY(), Point3D.disappointment.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);

		// Foge
		fugir();
	}

	// Ao bater numa parede
	public void onHitWall(HitWallEvent e) {
		emocaoActiva = new Point3D(Point3D.distress.getX(),
				Point3D.distress.getY(), Point3D.distress.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);

		reverterPosicao();
	}

	// Ao atingir outro Robô
	public void onHitRobot(HitRobotEvent roboAdv) {

		emocaoActiva = new Point3D(Point3D.fear.getX(), Point3D.fear.getY(),
				Point3D.fear.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);

		// Ajusta o radar e a arma
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		// Vira-se para o adversário que está a x angulos dele
		double x = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(x);

		// E dispara com a máxima potência
		fire(3);

		// Foge
		fugir();
	}

	// Dependendo da sua energia atual
	public void onStatus(StatusEvent e) {
		// Se tiver menos de 30% de energia está em risco de vida
		if (e.getStatus().getEnergy() < 30) {
			riscoVida = true;
		} else {
			riscoVida = false;
		}
		
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}

	// Fugir sempre que é atingido por uma bala ou quando vê um adversário
	public void fugir() {

		// Reverter a direção onde se encontra actualmente o robô
		reverterPosicao();
	}

	// O disparo depende da distância a que está o adversário
	public int forcaDisparo(double distancia) {
		if (distancia < 100) {
			return 3;
		} else if (distancia < 1000) {
			return 2;
		}
		return 1;

	}

	// Reverter a posição do robô
	public void reverterPosicao() {
		direcao *= -1;
		double d = (getDistanceRemaining() * direcao);

		setAhead(d);
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

		emocaoActiva = new Point3D(Point3D.joy.getX(), Point3D.joy.getY(),
				Point3D.joy.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onBulletMissed(BulletMissedEvent e) {

		emocaoActiva = new Point3D(Point3D.anger.getX(), Point3D.anger.getY(),
				Point3D.anger.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onDeath() {
		emocaoActiva = new Point3D(Point3D.remorse.getX(),
				Point3D.remorse.getY(), Point3D.remorse.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onWin() {
		emocaoActiva = new Point3D(Point3D.relief.getX(),
				Point3D.relief.getY(), Point3D.relief.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	// Ficar muito assustado sempre que a sua vida fica abaixo dos 30%
	public void muitoAssustado() {
		// Mudar a cor do robô
		setBodyColor(Color.white);
		setGunColor(Color.white);
		setRadarColor(Color.white);
		setBulletColor(Color.white);
		setScanColor(Color.white);

		// Ajusta o radar e a arma
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		setFire(1);
		setTurnRight(90);
		;

		waitFor(new TurnCompleteCondition(this));
		setFire(2);
		setTurnLeft(180);

		waitFor(new TurnCompleteCondition(this));
		setFire(3);
		setTurnRight(180);

		waitFor(new TurnCompleteCondition(this));
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