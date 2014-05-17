import oscP5.*;
import netP5.*;

class OscServer {
  OscP5 osc;
  NetAddress addr;

  public OscServer(){
    osc = new OscP5(this, 32001);
    addr = new NetAddress("127.0.0.1", 32000);
  }

  public void send(String name, int[] args){
    OscMessage msg = new OscMessage("/" + name);

    for (int s: args){
      msg.add(s);
    }

    osc.send(msg, addr);
  }

  /* incoming osc message are forwarded to the oscEvent method. */
  void oscEvent(OscMessage msg) {
    /* print the address pattern and the typetag of the received OscMessage */
    String pattern = msg.addrPattern();

    if (pattern.equals("/show_note")){
      // Global oops :(
      showNote();
    }
  }
}

