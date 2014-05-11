Particle particles[];
float minDistance = 120;
void setup(){
  size(displayWidth, displayHeight);

  int numParticles = 250;
  particles = new Particle[numParticles];
  for (int i = 0; i < numParticles; i++){
    particles[i] = new Particle();
  }
}

void draw(){
  background(0);

  strokeWeight(0);
  fill(255);
  stroke(255);

  for (int i = 0; i < particles.length; i++){
    particles[i].draw();

    // Now move
    particles[i].move();
  }

  // Draw lines between the particles based on distance
  for (int i = 0; i < particles.length; i++){
    for (int j = i + 1; j < particles.length; j++){
      Particle p1 = particles[i];
      Particle p2 = particles[j];

      float dist = p1.distanceFrom(p2);
      if (dist < minDistance){
        float alpha = scaleNum(0, minDistance, 255, 0, dist);
        float weight = scaleNum(0, minDistance, 4, 0, dist);

        stroke(255,255,255, alpha);
        strokeWeight(weight);
        line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
      }
    }
  }
}


float scaleNum(float inMin, float inMax, float outMin, float outMax, float x){
  float inNorm = inMax - inMin;
  float outNorm = outMax - outMin;
  float xNorm = x - inMin;

  return (xNorm/inNorm) * outNorm + outMin;
}

