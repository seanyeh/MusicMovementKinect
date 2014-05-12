import java.util.Arrays;


public int[] intify(PVector v){
  return new int[]{(int)v.x, (int)v.y, (int)v.z};
}

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

  private int[] getJointValue(SimpleOpenNI context, int joint){
    PVector j = new PVector();
    context.getJointPositionSkeleton(1, joint, j);
    return intify(j);
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
    else if (this.trackerType == JointTrackerType.GESTURE_SPEED){
      int[] j = getJointValue(context, this.joints[0]);

      this.gestureTracker.track(j);


      return this.gestureTracker.getSpeed();
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
  LinkedList<PVector> history;


  int HISTORY_NUM_FRAMES = 10;

  PVector totalMovement;

  public GestureTracker(){
    totalMovement = new PVector();

    // Track 10 frames at a time
    history = new LinkedList<PVector>();

  }

  public boolean track(int[] j){
    PVector vecJ = new PVector(j[0], j[1], j[2]);

    if (history.size() >= HISTORY_NUM_FRAMES){
      PVector old = history.remove();
      PVector newLast = (history.peekLast()).get();

      newLast.sub(old);
      totalMovement.sub(newLast);
    }

    if (history.size() >= 1){
      PVector oldFirst = history.peekFirst();

      PVector newMovementDelta = vecJ.get();
      newMovementDelta.sub(oldFirst);

      totalMovement.add(newMovementDelta);
    }
    history.add(vecJ);

    return true;
  }


  public int[] getSpeed(){
    PVector speed = totalMovement.get();
    speed.div(HISTORY_NUM_FRAMES);
    return intify(speed);
  }
}
