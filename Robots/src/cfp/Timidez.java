package cfp;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;

import ontology.Point3D;
import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

/* Car�ter de quem � t�mido; falta de coragem. Inseguran�a; acanhamento; inibi��o. s*/
public class Timidez extends AdvancedRobot { // Reactiva: apenas reage a a
												// determinadas situa��es

	// Posi��es do centro do campo de batalha
	double centroX;
	double centroY;

	// Energia do Inimigo que o est� a atacar
	double energiaInimigo = 100;

	// Dire��o oposta ao inimigo que est� a disparar
	int direcao = 1;

	// Direcionar a arma para o inimigo que est� a disparar
	int direcaoArma = -1;

	// Quando perto da parede, o rob� move-se para o centro evitando colis�es
	boolean pertoParede = false;

	// Variavel que determina quanta distancia o rob� tem de percorrer em cada
	// itera��o
	double distancia = 100;

	// Vari�veis para o Modelo OCC
	private double openness = 0.52;
	private double conscientiousness = 0.60;
	private double extraversion = 0.10;
	private double agreeableness = 0.30;
	private double neuroticism = 0.70;

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
		
		// Guarda a posi��o (X,Y) do centro do campo de batalha
		centroX = getBattleFieldWidth() / 2;
		centroY = getBattleFieldHeight() / 2;
		
		// Cores do rob�
		setBodyColor(Color.white);
		setGunColor(Color.white);
		setRadarColor(Color.white);
		setScanColor(Color.white);
		setBulletColor(Color.white);

		while (true) {

			turnRadarLeftRadians(1);

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

	// Ao bater num rob� guarda a sua energia e fica corado (muda de cor)
	public void onRobotHit(HitRobotEvent roboAdv) {
		// Fica Corado (muda de cor para vermelho)
		setBodyColor(Color.red);
		setGunColor(Color.red);
		setRadarColor(Color.red);
		setScanColor(Color.red);
		setBulletColor(Color.red);

		// Ajuda a manter a contagem de vida do inimigo correta
		this.energiaInimigo = roboAdv.getEnergy();

		// Volta ao seu estado normal
		setBodyColor(Color.white);
		setGunColor(Color.white);
		setRadarColor(Color.white);
		setScanColor(Color.white);
		setBulletColor(Color.white);

		// Vira-se para o advers�rio que est� a x angulos dele
		double z = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(z);

		// E dispara com a m�xima pot�ncia
		fire(3);

		// reverte a sua posi��o e foge
		reverterPosicao();
	}

	// Quando deteta outro rob�, tem de fazer opera��es de evas�o
	public void onScannedRobot(ScannedRobotEvent roboAdv) {
		/*
		 * Roda 30 graus em rela��o ao inimigo e ajusta a posi��o do rob� caso
		 * v� bater numa parede
		 */
		setTurnRight(roboAdv.getBearing() + 90 + (30 * this.direcao));
		detectarFogo(roboAdv);

		// Determina o poder de disaparo
		double poderDisparo = Math.min(500 / roboAdv.getDistance(), 3);
		fire(poderDisparo);

		// Vira-se para o advers�rio que est� a x angulos dele
		double z = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(z);
	}

	// Ao detetar uma quebra na energia (<=3) do rob� detectado muda a sua
	// dire��o
	public void detectarFogo(ScannedRobotEvent roboAdv) {

		double alteraoEnergia = this.energiaInimigo - roboAdv.getEnergy();
		if (alteraoEnergia > 0 && alteraoEnergia <= 3) {
			this.direcao *= -1;
			evitarParedes(roboAdv);
		}

		// Guarda a energia do inimigo
		this.energiaInimigo = roboAdv.getEnergy();
	}

	/*
	 * Calcula qual � a dist�ncia menor: se a dist�ncia para parede mais pr�xima
	 * ou mais ou menos 25% da dist�ncia at� ao rob� detectado, a dividir por 4.
	 * O objectivo � viver, e torna-se menos vuner�vel ao fogo evitando a
	 * colis�o com as paredes
	 */
	private void evitarParedes(ScannedRobotEvent roboAdv) {
		// guarda a posi��es do inimigo
		double advX = getX();
		double advY = getY();

		if (advX < 120 || advY < 120 || getBattleFieldHeight() - advY < 120
				|| getBattleFieldWidth() - advX < 120) {

			// Informa que j� est� perto de uma parede
			pertoParede = true;
		}

		// Se estiver a ir na dire��o da parede, vai para o centro
		if (pertoParede) {
			caminharParoCentro();
		} else {
			double dodgeDistance = Math
					.min(roboAdv.getDistance() / 4 + 25, 100);
			ahead(dodgeDistance);
		}
	}

	// Movimenta o rob� para o centro do campo de batalha
	public void caminharParoCentro() {
		boolean noCentro = false;

		// Se ainda n�o est� no centro
		if (!noCentro) {
			/*
			 * Descobre quanto tem de percorrer at� ao centro nos eixos dos X'se
			 * Y's
			 */
			double paraCentroX = centroX - getX();
			turnRight((paraCentroX > 0 ? 90 : -90) - getHeading());
			ahead((int) Math.abs(paraCentroX));

			double paraCentroY = centroY - getY();
			turnRight((paraCentroY > 0 ? 0 : 180) - getHeading());
			ahead((int) Math.abs(paraCentroY));
		}

		// Informa que j� n�o est� perto de uma parede
		pertoParede = false;
	}

	// Reverter a posi��o do rob�
	public void reverterPosicao() {
		double d = (getDistanceRemaining() * direcao);
		direcao *= -1;

		distancia = d;

		ahead(distancia);
	}

	// M�todos Adicionais do Modelo OCC
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