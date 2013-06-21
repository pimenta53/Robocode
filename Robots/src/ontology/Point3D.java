package ontology;

public class Point3D {
	
	public static Point3D joy = new Point3D(0.4, 0.2, 0.1);
	public static Point3D hope = new Point3D(0.2, 0.2, -0.1);
	public static Point3D relief = new Point3D(0.2, -0.3, 0.4);
	public static Point3D pride = new Point3D(0.4, 0.3, 0.3);
	public static Point3D gratitude = new Point3D(0.4, 0.2, -0.3);
	public static Point3D love = new Point3D(0.3, 0.1, 0.2);
	public static Point3D distress = new Point3D(-0.4, -0.2, -0.5);
	public static Point3D fear = new Point3D(-0.64, 0.6, -0.43);
	public static Point3D disappointment = new Point3D(-0.3, 0.1, -0.4);
	public static Point3D remorse = new Point3D(-0.3, 0.1, 0.6);
	public static Point3D anger = new Point3D(-0.51, 0.59, 0.25);
	public static Point3D hate = new Point3D(-0.6, 0.6, 0.3);
    
    public static final double screenDistance = 150.0;
    private double x;
    private double y;
    private double z;

    public Point3D(double x0, double y0, double z0) {
        x = x0;
        y = y0;
        z = z0;
    }

    public Point3D(int x0, int y0, int z0) {
        x = x0;
        y = y0;
        z = z0;
    }
    
    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(double value) {
        this.x = value;
    }

    public void setY(double value) {
        this.y = value;
    }

    public void setZ(double value) {
        this.z = value;
    }
}