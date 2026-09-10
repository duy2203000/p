import vn.gps.doduong.TripEngine;
import vn.gps.doduong.TripEngine.Fix;

public class TripEngineTest {
    static int checks=0;
    static void check(boolean ok,String name) { checks++; if(!ok) throw new AssertionError(name); }
    static void close(double actual,double expected,double tolerance,String name) { check(Math.abs(actual-expected)<=tolerance,name+": "+actual); }
    static Fix fix(double meters,long time,double speed) { return new Fix(0,Math.toDegrees(meters/6371008.8),3,time,speed,true,0.2,true); }
    static TripEngine ready() { TripEngine e=new TripEngine(); e.accept(fix(0,10000,0),10000); e.start(10000); return e; }
    public static void main(String[] args) {
        TripEngine e=ready();
        for(int i=1;i<=60;i++) e.accept(fix(i*10,10000+i*1000,10),10000+i*1000);
        close(e.meters,600,0.01,"600 m trajectory"); close(e.average(70000),36,0.01,"36 km/h average");
        close(e.maxKmh,36,0.001,"maximum speed"); e.stop(70000);
        check(e.elapsed(90000)==60000,"timer freezes after stop");
        e=ready(); for(int i=1;i<100;i++) e.accept(fix(i%2==0?1:-1,10000+i*1000,0),10000+i*1000);
        close(e.meters,0,0,"stationary drift suppressed");
        e=ready(); e.accept(fix(10,11000,10),11000); e.pause(11000);
        e.accept(fix(1000,21000,10),21000); check(e.resume(21000),"resume with fresh GPS");
        e.accept(fix(1010,22000,10),22000);
        close(e.meters,20,0.01,"exclude paused movement"); check(e.elapsed(22000)==2000,"exclude paused time");
        e=ready(); e.accept(fix(10,11000,10),11000); e.tick(17000);
        check(e.incomplete && !e.fresh(17000),"stale fix invalidated");
        e.accept(fix(1000,18000,10),18000); close(e.meters,10,0.01,"no bridge across missing GPS");
        e=ready(); e.accept(new Fix(0,0.1,100,11000,10,true,1,true),11000);
        check(e.incomplete && e.last==null,"weak GPS rejected");
        e.accept(fix(1000,12000,10),12000); close(e.meters,0,0.01,"no bridge across weak fixes");
        e=ready(); check(!e.accept(fix(5000,11000,10),11000),"teleport rejected"); close(e.meters,0,0,"teleport not counted");
        e=ready(); check(!e.accept(fix(10,10000,10),10000),"duplicate fix rejected");
        check(!e.accept(fix(10,9000,10),11000),"out of order rejected");
        check(!e.accept(fix(10,20000,10),12000),"future fix rejected");
        check(!e.accept(fix(10,11000,10),20000),"cached fix rejected");
        TripEngine blank=new TripEngine(); check(!blank.start(1000),"cannot start without GPS");
        e=ready(); e.pause(11000); check(!e.resume(20000),"cannot resume with stale GPS");
        e=ready(); e.accept(fix(10,11000,10),11000); e.accept(fix(10,12000,0),12000);
        close(e.average(12000),18,0.01,"average includes stationary time");
        e=ready(); e.accept(new Fix(0,Math.toDegrees(10/6371008.8),3,11000,0,false,0,false),11000);
        close(e.liveKmh,36,0.01,"fallback position speed");
        e=ready(); e.accept(new Fix(0,Double.NaN,3,11000,0,true,0,true),11000);
        check(e.last==null,"invalid coordinate rejected");
        close(TripEngine.distance(new Fix(0,179.999,3,1,0,false,0,false),new Fix(0,-179.999,3,2,0,false,0,false)),222.390,0.1,"antimeridian");
        e=ready(); e.accept(fix(10,11000,10),11000); e.stop(11000); e.accept(fix(20,12000,10),12000);
        check(e.start(12000),"new trip starts"); close(e.meters,0,0,"new trip clears prior distance");
        check(!e.incomplete && e.maxKmh==0,"new trip clears flags and max");
        System.out.println("PASS: "+checks+" assertions");
    }
}
