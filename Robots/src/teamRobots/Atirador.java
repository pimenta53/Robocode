package teamRobots;

import static robocode.util.Utils.normalRelativeAngleDegrees;

import java.awt.geom.Point2D;
import java.util.Random;

import ontology.Attack;
import ontology.Command;
import ontology.Help;
import ontology.Move;
import ontology.Point3D;
import ontology.Position;
import ontology.Shot;
import ontology.Target;

import robocode.BulletMissedEvent;
import robocode.Droid;
import robocode.HitByBulletEvent;
import robocode.MessageEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.TeamRobot;
import robocode.util.Utils;
import sampleteam.RobotColors;

public class Atirador extends TeamRobot{

	private Move move ;
	private Target target = null;

	//vari�veis usadas na previsao da trajetoria
	public static final int AVG_SIZE = 50;
	
	private double oldEnemyHeading=0;
	private Random gen;
	private double[] avgQ;
	private double avgV;
	private double avgVAbs;
	private int avgPos;
	

	//vari�vel que indica o algoritmo a usar na previsao
	private boolean linTargeting = true;
	private int missedBullets;

	private double openness = 0.87;
	private double conscientiousness = 0.63;
	private double extraversion = 0.22;
	private double agreeableness = 0.60;
	private double neuroticism = 0.42;
	private double Pbase = 0.59 * agreeableness + 0.19 * neuroticism + 0.21 * extraversion;
	private double Abase = -0.59 * neuroticism + 0.3 * agreeableness + 0.15 * openness;
	private double Dbase = 0.60 * extraversion - 0.32 * agreeableness + 0.25 * openness;
	private int t;
	private Point3D emocaoActiva;
	
	private double Pcurrent = Pbase, Acurrent = Abase, Dcurrent = Dbase;
	
	
	
	public void run(){
		gen = new Random();
        avgV = 4;
        avgVAbs = 4;
        avgQ = new double[AVG_SIZE];
        for (int j = 0; j < AVG_SIZE; j++) avgQ[j] = 4;
		missedBullets = 0;
		
		
		while(true){
			turnRadarLeft(360);
		}
	}
	
	public void onStatus(StatusEvent e){
		out.print("P A D base: \n" + "P: " + Pbase + "\n"
				  + "A: " + Abase + "\n" + "D: " + Dbase + "\n"
				  + "P A D correntes: \n" + "P: " + Pcurrent + "\n"
				  + "A: " + Acurrent + "\n" + "D: " + Dcurrent + "\n\n\n");
	}
	
	public void onMessageReceived(MessageEvent e) {
		// Fire at a point
		
		if (e.getMessage() instanceof Target) {
			
			 out.println("OK siraaa!!!!.");
			 Target t = (Target) e.getMessage();
			target=t;

			out.println("tenho de matar este"+t.getName());
			// Calculate x and y to target
				double dx = t.getP().getX() - this.getX();
				double dy = t.getP().getY() - this.getY();
				// Calculate angle to target
				double theta = Math.toDegrees(Math.atan2(dx, dy));

				turnGunRight(normalRelativeAngleDegrees(theta - getGunHeading()));
				// Fire hard!
				fire(3);

		 }
		if (e.getMessage() instanceof Position) {
			out.println("posição");
			Position p = (Position) e.getMessage();
			// Calculate x and y to target
			double dx = p.getX() - this.getX();
			double dy = p.getY() - this.getY();
			// Calculate angle to target
			double theta = Math.toDegrees(Math.atan2(dx, dy));

			move= new Move(new Position(p.getX(),p.getY()),new Position(getX(),getY()));
			
			// go to leader
			turnRight(normalRelativeAngleDegrees(theta - getGunHeading()));
			ahead(move.move()-100);
		} // Set our colors
		 if (e.getMessage() instanceof RobotColors) {
			RobotColors c = (RobotColors) e.getMessage();

			out.println("cores");
			setBodyColor(c.bodyColor);
			setGunColor(c.gunColor);
			setRadarColor(c.radarColor);
			setScanColor(c.scanColor);
			setBulletColor(c.bulletColor);
		}
	}
		 /*
		 if (e.getMessage() instanceof Help) {
			
			 out.println("OK sir!!!!.");
			 Help h = (Help) e.getMessage();
				// Calculate x and y to target
				double dx = h.getP().getX() - this.getX();
				double dy = h.getP().getY() - this.getY();
				// Calculate angle to target
				double theta = Math.toDegrees(Math.atan2(dx, dy));

				turnGunRight(normalRelativeAngleDegrees(theta - getGunHeading()+h.getD()));
				// Fire hard!
				fire(3);
		}
		 if (e.getMessage() instanceof Command) {
			
		 }
		 
	}

	*/
	
	
	//Algoritmo que assume que o movimento do robot adversario � linear
	
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

		//Algoritmo que assume que o movimento do robot adversario � circular
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
			
			// Don't fire on teammates
			if (isTeammate(e.getName())) {
				return;
			}
			// !e.getName().equals(target.getName())
			//target = e.getName();
			
			// Calculate enemy bearing
			double enemyBearing = this.getHeading() + e.getBearing();
			// Calculate enemy's position
			double enemyX = getX() + e.getDistance() * Math.sin(Math.toRadians(enemyBearing));
			double enemyY = getY() + e.getDistance() * Math.cos(Math.toRadians(enemyBearing));
			
			
			Target tg = new Target(e.getName(),e.getEnergy(),e.getClass().getName(),new Position(enemyX,enemyY));
			
			if(tg.getPower()>=80 && !isTeammate(tg.getName())) target=tg;
			out.println("fire" + tg.getName());
			if(target!=null) fireT(e);
		scan();
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
		}

	public void fireT(ScannedRobotEvent e){
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
		}
		else circularTargeting(e);
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
		ahead(100);
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
