class Particle {
  int MAX_X = width;
  int MAX_Y = height;

  private float x, y;
  private float xVel, yVel;
  private float radius = 6;

  public Particle(){
    x = random(0, MAX_X);
    y = random(0, MAX_Y);
    xVel = random(-2.0, 2.0);
    yVel = random(-2.0, 2.0);
  }

  public void move(){
    x = (x + xVel)% MAX_X;
    y = (y + yVel)% MAX_Y;
  }

  public void draw(){
    ellipse(x, y, radius, radius);
  }

  public float distanceFrom(Particle p){
    float xDelta = p.getX() - this.getX();
    float yDelta = p.getY() - this.getY();
    return sqrt(xDelta * xDelta + yDelta * yDelta);
  }

  public float getX(){ return x; }
  public float getY(){ return y; }
}
