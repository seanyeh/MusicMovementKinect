class NoteFlower{

  float x, y;
  int circleRadius = 80;
  int radius = 45;

  float opacity = 150;

  float r,g,b;
  float rotPos;
  boolean isDead = false;
  float xVel, yVel;
  float rotVel;
  public NoteFlower(float cx, float cy){
    x = cx;
    y = cy;

    r = random(256);
    g = random(256);
    b = random(256);

    xVel = random(0,2.0) + 1;
    yVel = random(0,2.0) + 1;

    rotPos = 0;
    rotVel = random(1,4);
  }

  public NoteFlower(){
    this(0,0);
  }

  public void draw(){
    if (isDead){ return; }
    noStroke();

    fill(r, g, b, opacity);

    for (int i = 0; i < 4; i++){
      float[] offsets = radiansToXY(radians(rotPos + 90*i));
      ellipse(x + offsets[0], y + offsets[1], circleRadius, circleRadius);
    }

  }

  public void age(){
    rotPos += rotVel;
    x += xVel;
    y += yVel;
    
    if (x < 0 || x > displayWidth || y < 0 || y > displayHeight){
      isDead = true;
    }
  }

  private float[] radiansToXY(float rads){
    float x = cos(rads);
    float y = sin(rads);
    return new float[]{x * radius, y * radius};
  }
}

