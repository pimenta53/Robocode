package cfp;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.Color;

import ontology.Point3D;
import robocode.AdvancedRobot;
import robocode.HitRobotEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

/* Caráter de quem é tímido; falta de coragem. Insegurança; acanhamento; inibição. s*/
public class Timidez extends AdvancedRobot { // Reactiva: apenas reage a a
												// determinadas situações

	// Posições do centro do campo de batalha
	double centroX;
	double centroY;

	// Energia do Inimigo que o está a atacar
	double energiaInimigo = 100;

	// Direção oposta ao inimigo que está a disparar
	int direcao = 1;

	// Direcionar a arma para o inimigo que está a disparar
	int direcaoArma = -1;

	// Quando perto da parede, o robô move-se para o centro evitando colisões
	boolean pertoParede = false;

	// Variavel que determina quanta distancia o robô tem de percorrer em cada
	// iteração
	double distancia = 100;

	// Variáveis para o Modelo OCC
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
		
		// Guarda a posição (X,Y) do centro do campo de batalha
		centroX = getBattleFieldWidth() / 2;
		centroY = getBattleFieldHeight() / 2;
		
		// Cores do robô
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

	// Ao bater num robô guarda a sua energia e fica corado (muda de cor)
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

		// Vira-se para o adversário que está a x angulos dele
		double z = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(z);

		// E dispara com a máxima potência
		fire(3);

		// reverte a sua posição e foge
		reverterPosicao();
	}

	// Quando deteta outro robô, tem de fazer operações de evasão
	public void onScannedRobot(ScannedRobotEvent roboAdv) {
		/*
		 * Roda 30 graus em relação ao inimigo e ajusta a posição do robô caso
		 * vá bater numa parede
		 */
		setTurnRight(roboAdv.getBearing() + 90 + (30 * this.direcao));
		detectarFogo(roboAdv);

		// Determina o poder de disaparo
		double poderDisparo = Math.min(500 / roboAdv.getDistance(), 3);
		fire(poderDisparo);

		// Vira-se para o adversário que está a x angulos dele
		double z = normalRelativeAngleDegrees(roboAdv.getBearing()
				+ (getHeading() - getRadarHeading()));
		turnGunRight(z);
	}

	// Ao detetar uma quebra na energia (<=3) do robô detectado muda a sua
	// direção
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
	 * Calcula qual é a distância menor: se a distância para parede mais próxima
	 * ou mais ou menos 25% da distância até ao robô detectado, a dividir por 4.
	 * O objectivo é viver, e torna-se menos vunerável ao fogo evitando a
	 * colisão com as paredes
	 */
	private void evitarParedes(ScannedRobotEvent roboAdv) {
		// guarda a posições do inimigo
		double advX = getX();
		double advY = getY();

		if (advX < 120 || advY < 120 || getBattleFieldHeight() - advY < 120
				|| getBattleFieldWidth() - advX < 120) {

			// Informa que já está perto de uma parede
			pertoParede = true;
		}

		// Se estiver a ir na direção da parede, vai para o centro
		if (pertoParede) {
			caminharParoCentro();
		} else {
			double dodgeDistance = Math
					.min(roboAdv.getDistance() / 4 + 25, 100);
			ahead(dodgeDistance);
		}
	}

	// Movimenta o robô para o centro do campo de batalha
	public void caminharParoCentro() {
		boolean noCentro = false;

		// Se ainda não está no centro
		if (!noCentro) {
			/*
			 * Descobre quanto tem de percorrer até ao centro nos eixos dos X'se
			 * Y's
			 */
			double paraCentroX = centroX - getX();
			turnRight((paraCentroX > 0 ? 90 : -90) - getHeading());
			ahead((int) Math.abs(paraCentroX));

			double paraCentroY = centroY - getY();
			turnRight((paraCentroY > 0 ? 0 : 180) - getHeading());
			ahead((int) Math.abs(paraCentroY));
		}

		// Informa que já não está perto de uma parede
		pertoParede = false;
	}

	// Reverter a posição do robô
	public void reverterPosicao() {
		double d = (getDistanceRemaining() * direcao);
		direcao *= -1;

		distancia = d;

		ahead(distancia);
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