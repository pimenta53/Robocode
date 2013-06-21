package cfp;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.Random;

import ontology.Point3D;

import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.util.Utils;

public class Pontaria extends AdvancedRobot { //Deliberativa - vai para um canto e dispara com poder de 3 quando detecta um robot
											  //consoante o numero de tiros falhados, pode alterar entre o modo linear e o circular
	
	//vari‡vel para verificar se jogou bem
	private int robotSurv = 0;
	//vari‡vel que indica o canto para onde se dirige
	private int corner = 0;
	//vari‡veis usadas na previsao da trajetoria
	public static final int AVG_SIZE = 50;
	private Random gen;
    private double[] avgQ;
    private double avgV;
    private double avgVAbs;
    private int avgPos;
	private double oldEnemyHeading=0;
	//vari‡vel que indica o algoritmo a usar na previsao
	private boolean linTargeting = true;
	private int missedBullets;

	private double openness = 0.87;
	private double conscientiousness = 0.63;
	private double extraversion = 0.22;
	private double agreeableness = 0.48;
	private double neuroticism = 0.42;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
	
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	public void run(){
		// Define as cores do robot
		setBodyColor(Color.blue);
		setGunColor(Color.blue);
		setRadarColor(Color.blue);
		setScanColor(Color.blue);
		setBulletColor(Color.blue);
		
		robotSurv = getOthers();
		gen = new Random();
        avgV = 4;
        avgVAbs = 4;
        avgQ = new double[AVG_SIZE];
        for (int j = 0; j < AVG_SIZE; j++) avgQ[j] = 4;
		missedBullets = 0;
		
		goCorner();	
		setAdjustGunForRobotTurn(true); 
	    setAdjustRadarForGunTurn(true); 
	    setTurnRadarRight(Double.POSITIVE_INFINITY);
	    while(true){
	    	if (corner == 0 || corner == 270) turnRadarRight(90);
	    	else if (corner == 90 || corner == 180) turnRadarLeft(90);
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
	
	public void goCorner() {
        // vira o robot para a parede ˆ direita do canto pretendido
        turnRight(Utils.normalRelativeAngleDegrees(corner - getHeading()));
        // Movimentos para se aproximar do canto
        if (corner == 0){
	        ahead(getBattleFieldHeight()-getY()-20);
	        turnLeft(90);
	        ahead(getX()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()) + 90);
        }
        else if (corner == 90){
        	ahead(getBattleFieldWidth()-getX()-20);
	        turnLeft(90);
	        ahead(getBattleFieldHeight()-getY()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()) - 90);
        }
        else if (corner == 180){
        	ahead(getY()-20);
	        turnLeft(90);
	        ahead(getBattleFieldWidth()-getX()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()));
        }
        else if (corner == 270){
        	ahead(getX()-20);
	        turnLeft(90);
	        ahead(getY()-20);
	        turnGunRight(Utils.normalRelativeAngleDegrees(corner - getHeading()));
        }
	}
	
	/**
	 * onDeath: verifica se o turno correu bem: se quando morreu, 75% dos robots adversarios ja ainda estavam vivos, entao muda
	 * 			de canto. Caso contrario, mantem-se no mesmo
	 */
	public void onDeath(DeathEvent e) {

        if (robotSurv == 0) {
    		emocaoActiva = new Point3D(Point3D.relief.getX(),Point3D.relief.getY(),Point3D.relief.getZ());
    		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
    		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
        	return;
        }
        if ((robotSurv - getOthers()) / (double) robotSurv < .75) {
        	corner += 90;
            if (corner == 270) corner = -90;

    		emocaoActiva = new Point3D(Point3D.hate.getX(),Point3D.hate.getY(),Point3D.hate.getZ());
    		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
    		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
        }
	}
	
	//Algoritmo que assume que o movimento do robot adversario Ž linear
	
	private void linearTargeting(ScannedRobotEvent e){
		
		double gunDir = getGunHeading(), d = e.getDistance(), hT = e.getHeadingRadians();
        //bearing para o alvo inicial
        double bTo = Math.toRadians(fixAngle(e.getBearing() + getHeading()));
        double xo = getX(), yo = getY();
        double xTo = d * Math.sin(bTo) + xo; 
        double yTo = d * Math.cos(bTo) + yo; 
        double vT = averageVelocity(e.getVelocity()); 
        double gunTurnSpeed = 20;
        double time = d / 11; 
        double timeCur = time; 

        double x = 0, xCheck = 0, y = 0, bT = 0;
        
        for (int tryNum = 1; tryNum < 30; tryNum++){
        	x = vT * timeCur * Math.sin(hT) + xTo; 
            y = vT * timeCur * Math.cos(hT) + yTo; 
            bT = fixAngleRad(Math.atan2((x - xo),(y - yo))); 
            
            if(getTurnRemaining()*(bT-Math.toRadians(gunDir)) > 0) gunTurnSpeed = 30 - .75 * Math.abs(getVelocity());
            else gunTurnSpeed = 10 + .75 * Math.abs(getVelocity());
               
            xCheck = 11 * (timeCur - Math.abs(fixAngle(Math.toDegrees(bT) - gunDir)) / gunTurnSpeed) * Math.sin(bT) + xo;
            if (((xCheck - xo) / (x - xo)) > 1) timeCur = timeCur - time / Math.pow(2, tryNum);
            else timeCur = timeCur + time / Math.pow(2, tryNum);
        }
        setTurnGunRight(fixAngle(Math.toDegrees(bT) - gunDir)); 
	}

	//Algoritmo que assume que o movimento do robot adversario Ž circular
	private void circularTargeting(ScannedRobotEvent e){
		
		double bulletPower = Math.min(3.0,getEnergy());
		double myX = getX(), myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		double enemyVelocity = e.getVelocity();
		 
		double deltaTime = 0, predictedX = enemyX, predictedY = enemyY;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
		 	predictedX += Math.sin(enemyHeading) * enemyVelocity;
		 	predictedY += Math.cos(enemyHeading) * enemyVelocity;
		 	enemyHeading += enemyHeadingChange;
		 	if(predictedX < 18.0 || predictedY < 18.0 || predictedX > battleFieldWidth - 18.0 || predictedY > battleFieldHeight - 18.0){
		  
		 		predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);	
		 		predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
		 		break;
		 	}
		 }
		 double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
		  
		 setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		 setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
		 fire(3);

		emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		oldEnemyHeading = enemyHeading;
	}
	
	//normaliza o angulo passado como argumento (graus)
	private double fixAngle(double angle){
		
		while (angle <= -180) angle = angle + 360;
        while (angle > 180) angle = angle - 360;
        return angle;
    }
	
	//devolve a velocidade media do robot
	private double averageVelocity(double velocityReal){
		
		double seed = gen.nextDouble()*100;
		if(seed < 40) return avgV;
		if(seed < 75) return avgVAbs;
		return velocityReal;
    }
	
	//guarda e atualiza os valores relacionados com a velocidade do robot
	private void adjustAvg(double newV){
		avgV += (newV - avgQ[avgPos]) / AVG_SIZE;
        avgVAbs += (Math.abs(newV) - Math.abs(avgQ[avgPos])) / AVG_SIZE;
        avgQ[avgPos] = newV;
        avgPos++;
        if (avgPos >= AVG_SIZE) avgPos = 0;
    }
	
	//normaliza o angulo passad como argumento (radianos)
	private double fixAngleRad(double angle){
		
		while (angle <= -Math.PI) angle = angle + 2 * Math.PI;
		while (angle > Math.PI) angle = angle - 2 * Math.PI;
		return angle;
    }
	
	//ao detetar um robot, dispara um tiro sobre este consoante o algoritmo ativo
	public void onScannedRobot(ScannedRobotEvent e) {
		
		if(linTargeting){
			double distance = e.getDistance();
            double absoluteBearing = e.getBearingRadians() + getHeadingRadians();
            double heading = e.getHeadingRadians();
            double velocity = e.getVelocity();
            double aimX = Math.sin(absoluteBearing)*distance + Math.sin(heading)*velocity - Math.sin(getHeadingRadians())*getVelocity();
            double aimY = Math.cos(absoluteBearing)*distance + Math.cos(heading)*velocity - Math.cos(getHeadingRadians())*getVelocity();
            
            setTurnRadarRightRadians(fixAngleRad(Math.atan2(aimX, aimY)-getRadarHeadingRadians()));
           	adjustAvg(e.getVelocity());
           	linearTargeting(e);
           	fire(3);

    		emocaoActiva = new Point3D(Point3D.hope.getX(),Point3D.hope.getY(),Point3D.hope.getZ());
    		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
    		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
		}
        else circularTargeting(e);
	}
	
	/**
	 * onBulletMissed: sempre que falha um tiro, incrementa o contador e vai verificar se deve alterar o algoritmo preditivo a usar
	 */
	public void onBulletMissed(BulletMissedEvent e){
		missedBullets++;
		if(linTargeting && missedBullets > 5){
			linTargeting = false;
			missedBullets = 0;
		}
		else if(!linTargeting && missedBullets > 5){
			linTargeting = true;
			missedBullets = 0;
		}
		
		emocaoActiva = new Point3D(Point3D.distress.getX(),Point3D.distress.getY(),Point3D.distress.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);

	}
	
	public void onHitByBullet(HitByBulletEvent e) {

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

		emocaoActiva = new Point3D(Point3D.joy.getX(),Point3D.joy.getY(),Point3D.joy.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onWin(){
		emocaoActiva = new Point3D(Point3D.pride.getX(),Point3D.pride.getY(),Point3D.pride.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}
	
	public void onHitWall(HitWallEvent e){
		emocaoActiva = new Point3D(Point3D.remorse.getX(),Point3D.remorse.getY(),Point3D.remorse.getZ());
		double intense = getEmotionIntensity(emocaoActiva.getX(), emocaoActiva.getY(), emocaoActiva.getZ());
		if (intense > Math.abs(neuroticism-extraversion)) updateEmotion(emocaoActiva,intense,0);
	}	
	
	public void onHitRobot(HitRobotEvent e) {
		
		emocaoActiva = new Point3D(Point3D.hate.getX(),Point3D.hate.getY(),Point3D.hate.getZ());
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
