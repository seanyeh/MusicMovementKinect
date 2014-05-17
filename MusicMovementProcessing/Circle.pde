class Circle{

  float x, y;
  int radius = 40;
  int blurWidth = 40;

  float opacityRatio = 0.8;
  float greenNum = 0;
  public Circle(float cx, float cy){
    x = cx;
    y = cy;
  }


  public void draw(){
    noStroke();

    for (int i = 1; i <= blurWidth; i+=2){
      float opacity = scaleNum(0, blurWidth, 150, 20, i);
      fill(255, greenNum, 0, opacity * opacityRatio);
      ellipse(x, y, radius + i, radius + i);
    }

    fill(255, greenNum, 0, 200 * opacityRatio);
    ellipse(x, y, radius, radius);
  }

  public void age(){
    opacityRatio = opacityRatio * 0.9;
    greenNum += 10;
    radius += 2;
  }


}
