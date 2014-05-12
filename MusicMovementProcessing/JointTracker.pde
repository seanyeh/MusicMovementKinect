import java.util.Arrays;

class JointTracker {
  int[] pos;
  int[] joints;
  JointTrackerType trackerType;

  /* Only used for gestures */
  GestureTracker gestureTracker;
  boolean gestureComplete;

  boolean isUpdated;
  String name;

  int MAX = 1600;


  public JointTracker(String name, JointTrackerType trackerType, int ... joints){
    this.name = name;
    this.trackerType = trackerType;
    this.joints = joints;

    this.isUpdated = false;
    this.pos = new int[3];

    this.gestureTracker = new GestureTracker();
    this.gestureComplete = false;
  }

  public JointTracker(String name, int joint){
    this(name, JointTrackerType.POS, joint);
  }

  // Convert value to range from 0 to MAX
  public int normalizeValue(double d){
    int i = (int)d + MAX/2;
    /* if (i < 0){ */
    /*   return 0; */
    /* } else if (i > MAX){ */
    /*   return MAX; */
    /* } else{ */
    /*   return i; */
    /* } */
    return (int)d;
  }



  private int[] getJointValue(SimpleOpenNI context, int joint){
    PVector j = new PVector();
    context.getJointPositionSkeleton(1, joint, j);
    return new int[]{(int)j.x, (int)j.y, (int)j.z};
  }

  public int[] getValue(SimpleOpenNI context){
    if (this.trackerType == JointTrackerType.POS){
      return getJointValue(context, this.joints[0]);
    } 
    else if (this.trackerType == JointTrackerType.DIFF){
      assert this.joints.length == 2;

      int[] j1 = getJointValue(context, this.joints[0]);
      int[] j2 = getJointValue(context, this.joints[1]);

      return new int[]{j1[0] - j2[0], j1[1] - j2[1], j1[2] - j2[2]};
    }
    else if (this.trackerType == JointTrackerType.GESTURE_HAND1){
      int[] j = getJointValue(context, this.joints[0]);

      boolean trackResult = this.gestureTracker.track(j);
      if (trackResult){
        this.gestureComplete = true;
      }
      /* System.out.println(this.gestureTracker.getSpeed()); */

      return new int[]{0,0,0};
    }
    else{
      System.err.println("Not Valid JointTrackerType!");
      return null;
    }
  }


  public void update(SimpleOpenNI context){
    int[] newPos = getValue(context);

    if (Arrays.equals(pos, newPos)){
      this.isUpdated = false;
    } else{
      this.isUpdated = true;
      this.pos = newPos;
    }
  }

  public boolean isUpdated(){ return this.isUpdated; }
  public void setUpdated(boolean b){ this.isUpdated = b; }

  /* such a hack fix this later please */
  public boolean isGestureComplete(){ 
    boolean b = this.gestureComplete;
    this.gestureComplete = false;
    return b;
  } 

  public int[] getPos(){ 
    return this.pos;
  }
  public String getName(){ return this.name; }
}

class GestureTracker {

  int[] joints;

  LinkedList<Integer> history;
  LinkedList<Integer> deltaHistory;

  GestureTrackerPhase phase;

  int GESTURE_MAX_FRAMES = 10;
  int frames = 0;
  

  /* int speed; */
  int totalMovement;

  public GestureTracker(){
    joints = new int[3];
    totalMovement = 0;

    // Track 10 frames at a time
    history = new LinkedList<Integer>();
    deltaHistory = new LinkedList<Integer>();

    phase = GestureTrackerPhase.NONE;
    frames = 0;
  }

  public boolean track(int[] j){
    // Track only z for now
    if (history.size() >= 10){
      int old = history.remove();
      int newLast = history.peekLast();

      totalMovement -= newLast - old;
    }

    if (history.size() >= 1){
      int oldFirst = history.peekFirst();
      int newMovementDelta = j[2] - oldFirst;

      totalMovement += newMovementDelta;
    }
    history.add(j[2]);

    // Track phase
    int speed = getSpeed();
    if (speed < -30) {
      frames = 0;
      phase = GestureTrackerPhase.DEC;
      /* System.out.println("DEC!"); */
    }
    else if (speed > 30){
      if (phase == GestureTrackerPhase.DEC && frames >= 4){
        System.out.println("DETECTED :) frames: " + frames);
        frames = 0;
        return true;
      }
      /* System.out.println("INC!"); */
      phase = GestureTrackerPhase.INC;
    }
    else{
      // If first phase started but didn't complete
      if (phase == GestureTrackerPhase.DEC){
        frames++;
      }

      if (frames > GESTURE_MAX_FRAMES){
        System.out.println("Lost gesture!");
        frames = 0;
        phase = GestureTrackerPhase.NONE;
      }
    }

    return false;
  }


  public int getSpeed(){
    return totalMovement/10;
  }
}
