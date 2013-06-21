package cfp;

import robocode.*;
import robocode.util.Utils;
import java.awt.Color;

import ontology.Point3D;

/* Temor - Ato ou efeito de temer; receio, susto, medo, terror: 
 * viver no temor da miséria, da velhice, da morte (reactiva)
 */
public class Temor extends AdvancedRobot {

	// Variavel que determina quanto o robô tem de distancia em cada iteração
	double distancia = 100;

	// Variáveis que determinam o estado do robô em cada instante
	private boolean verMorte = false;
	private boolean temerMorte = true;
	private boolean noCanto = false;

	// Direção do robô
	private int direcao = 1;

	// Variáveis para o Modelo OCC
	private double openness = 0.10;
	private double conscientiousness = 0.40;
	private double extraversion = 0.20;
	private double agreeableness = 0.20;
	private double neuroticism = 0.85;

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

		// Cores do robô
		setColors(Color.gray, Color.gray, Color.gray);
		setBulletColor(Color.gray);

		// Enquanto for verdade
		while (true) {

			turnRadarLeftRadians(1);
			ahead(distancia);

			if (emocaoActiva != null) {
				double intense = getEmotionIntensity(emocaoActiva.getX(),
						emocaoActiva.getY(), emocaoActiva.getZ());
				if (intense > Math.abs(neuroticism - extraversion))
					updateEmotion(emocaoActiva, intense,
							((int) System.currentTimeMillis() / 100) - t);
			}
		}

	}

	// Ao bater numa parede
	public void onHitWall(HitWallEvent e) {

		emocaoActiva = new Point3D(Point3D.fear.getX(), Point3D.fear.getY(),
				Point3D.fear.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);

		if (temerMorte)
			reverterPosicao();
	}

	// Ao bater noutro robô (Morte)
	public void onHitRobot(HitRobotEvent e) {
		// Reverte a sua posição e foge
		if (temerMorte) {

			emocaoActiva = new Point3D(Point3D.distress.getX(),
					Point3D.distress.getY(), Point3D.distress.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(),
					emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism - extraversion))
				updateEmotion(emocaoActiva, intense, 0);

			// reverte a sua posição e foge
			reverterPosicao();
		}
		// Senão se já tiver a ver a morte
		else if (verMorte) {

			emocaoActiva = new Point3D(Point3D.fear.getX(),
					Point3D.fear.getY(), Point3D.fear.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(),
					emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism - extraversion))
				updateEmotion(emocaoActiva, intense, 0);

			// Espera que a morte o leve (espera pela sua morte)
			return;
		}
	}

	// Ao detetar um robô (Morte)
	public void onScannedRobot(ScannedRobotEvent e) {
		// Se tiver menos de 30% de vida
		if (verMorte) {

			emocaoActiva = new Point3D(Point3D.fear.getX(),
					Point3D.fear.getY(), Point3D.fear.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(),
					emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism - extraversion))
				updateEmotion(emocaoActiva, intense, 0);

			setTurnGunRightRadians(Utils
					.normalRelativeAngle((getHeadingRadians() + e
							.getBearingRadians()) - getGunHeadingRadians()));

			// Caminha/Foge para a parede mais próxima
			irParedeMaisProxima();
		}
		// Senão se tiver entre 30% e 80% da vida (continua a lutar contra a
		// Morte)
		else if (temerMorte) {

			emocaoActiva = new Point3D(Point3D.distress.getX(),
					Point3D.distress.getY(), Point3D.distress.getZ());
			double intense = getEmotionIntensity(emocaoActiva.getX(),
					emocaoActiva.getY(), emocaoActiva.getZ());
			if (intense > Math.abs(neuroticism - extraversion))
				updateEmotion(emocaoActiva, intense, 0);

			// Ajusta o radar e a arma
			setAdjustGunForRobotTurn(true);
			setAdjustRadarForGunTurn(true);

			setTurnGunRightRadians(Utils
					.normalRelativeAngle((getHeadingRadians() + e
							.getBearingRadians()) - getGunHeadingRadians()));

			// Dispara contra a morte e foge
			fire(3);

			// Fugir da morte
			fugirMorte(distancia);

			// Caminhar para o canto mais perto
			caminharParaCanto();
		}
	}

	// Dependendo da percentagem da sua energia
	public void onStatus(StatusEvent e) {
		// Se a a energia for menor que 80%
		if (e.getStatus().getEnergy() < 80) {
			// Começa a temer pela sua morte
			temerMorte = true;
		}
		// Senão se a sua energia for menor que 30%
		else if (e.getStatus().getEnergy() < 30) {
			// começa a ver a morte
			verMorte = false;
		}

		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
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

		emocaoActiva = new Point3D(Point3D.pride.getX(), Point3D.pride.getY(),
				Point3D.pride.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onBulletMissed(BulletMissedEvent e) {

		emocaoActiva = new Point3D(Point3D.disappointment.getX(),
				Point3D.disappointment.getY(), Point3D.disappointment.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onDeath() {
		emocaoActiva = new Point3D(Point3D.anger.getX(), Point3D.anger.getY(),
				Point3D.anger.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	public void onWin() {
		emocaoActiva = new Point3D(Point3D.gratitude.getX(),
				Point3D.gratitude.getY(), Point3D.gratitude.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(),
				emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism - extraversion))
			updateEmotion(emocaoActiva, intense, 0);
	}

	// Reverter a posição do robô
	public void reverterPosicao() {
		double d = (getDistanceRemaining() * direcao);
		direcao *= -1;

		distancia = d;

		setAhead(distancia);
	}

	// Fugir da Morte
	public void fugirMorte(double distancia) {
		setAhead(distancia);
		setTurnRight(distancia);
	}

	// Caminhar para o Canto mais próximo
	public void caminharParaCanto() {

		if (getDistanceRemaining() == 0 && getTurnRemaining() == 0) {
			if (noCanto) {

			} else {

				if ((getHeading() % 90) != 0) {
					setTurnLeft((getY() > (getBattleFieldHeight() / 2)) ? getHeading()
							: getHeading() - 180);
					waitFor(new TurnCompleteCondition(this));
				}

				else if (getY() > 30 && getY() < getBattleFieldHeight() - 30) {

					setAhead(getHeading() > 90 ? substituirVarAndar(getY() - 20)
							: substituirVarAndar(getBattleFieldHeight()
									- getY() - 20));
				}

				else if (getHeading() != 90 && getHeading() != 270) {
					if (getX() < 350) {
						setTurnLeft(getY() > 300 ? 90 : -90);
						waitFor(new TurnCompleteCondition(this));
					} else {
						setTurnLeft(getY() > 300 ? -90 : 90);
						waitFor(new TurnCompleteCondition(this));
					}
				}

				else if (getX() > 30 && getX() < getBattleFieldWidth() - 30) {
					setAhead(getHeading() < 180 ? substituirVarAndar(getX() - 20)
							: substituirVarAndar(getBattleFieldWidth() - getX()
									- 20));
				}

				else if (getHeading() == 270) {
					setTurnGunLeft(getY() > 200 ? 90 : 180);
					noCanto = true;
					// setMaxVelocity(0);

				}

				else if (getHeading() == 90) {
					setTurnGunLeft(getY() > 200 ? 180 : 90);
					noCanto = true;
					// setMaxVelocity(0);

				}
			}
		}
	}

	// Auxiliar
	public double substituirVarAndar(double valor) {
		distancia = valor;
		return distancia;

	}

	// O robot já se encontra na Parede?
	public boolean naParede() {
		double fieldHeight = getBattleFieldHeight();
		double fieldWidth = getBattleFieldWidth();
		double robotX = getX();
		double robotY = getY();

		int t_wall = 36;

		if (robotX < t_wall) {
			return true;
		}

		if (robotY < t_wall) {
			return true;
		}

		if (fieldWidth - robotX < t_wall) {
			return true;
		}

		if (fieldHeight - robotY < t_wall) {
			return true;
		}

		return false;
	}

	public void irParedeMaisProxima() {
		double fieldHeight = getBattleFieldHeight();
		double fieldWidth = getBattleFieldWidth();
		double robotX = getX();
		double robotY = getY();

		if (naParede())
			return;

		double nearest_x, nearest_y, turn_x, turn_y, gun_x, gun_y;

		if (fieldWidth - robotX > fieldWidth / 2) {

			nearest_x = robotX;
			turn_x = 270;
			gun_x = 90;
		} else {

			nearest_x = fieldWidth - robotX;
			turn_x = 90;
			gun_x = 270;
		}

		if (fieldHeight - robotY > fieldHeight / 2) {

			nearest_y = robotY;
			turn_y = 180;
			gun_y = 0;
		} else {

			nearest_y = fieldHeight - robotY;
			turn_y = 0;
			gun_y = 180;
		}

		if (nearest_x < nearest_y) {
			turnLeft(turn_x);
			distancia = nearest_x;
			ahead(distancia);
			turnLeft(0);
			turnGunLeft(gun_x);
		} else {
			turnLeft(turn_y);
			distancia = nearest_y;
			ahead(distancia);
			turnLeft(90);
			turnGunLeft(gun_y);
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
