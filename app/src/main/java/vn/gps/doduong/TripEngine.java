package vn.gps.doduong;

/** Pure Java measurement logic. All times use Android elapsedRealtime, never wall clock. */
public final class TripEngine {
    public static final long FRESH_MS = 5000, GAP_MS = FRESH_MS;
    public static final double MAX_ACCURACY = 25, MAX_SPEED = 100; // m, m/s (360 km/h)
    public static final class Fix {
        public final double lat, lon, accuracy, speed, speedAccuracy;
        public final long time;
        public final boolean hasSpeed, hasSpeedAccuracy;
        public Fix(double lat, double lon, double accuracy, long time, double speed,
                   boolean hasSpeed, double speedAccuracy, boolean hasSpeedAccuracy) {
            this.lat=lat; this.lon=lon; this.accuracy=accuracy; this.time=time;
            this.speed=speed; this.hasSpeed=hasSpeed;
            this.speedAccuracy=speedAccuracy; this.hasSpeedAccuracy=hasSpeedAccuracy;
        }
    }
    public boolean active, paused, incomplete;
    public double meters, maxKmh, liveKmh = Double.NaN;
    public String status = "Đang chờ GPS…";
    public Fix last;
    private Fix anchor;
    private long started, accumulated, lastInput = -1;

    public boolean fresh(long now) { return last != null && now >= last.time && now-last.time <= FRESH_MS; }
    public long elapsed(long now) { return accumulated + (active && !paused ? Math.max(0, now-started) : 0); }
    public double average(long now) { long ms=elapsed(now); return ms>0 ? meters*3600/ms : 0; }
    public boolean start(long now) {
        if (active || !fresh(now)) return false;
        active=true; paused=false; incomplete=false; meters=0; maxKmh=0;
        accumulated=0; started=now; anchor=last; return true;
    }
    public void pause(long now) {
        if (!active || paused) return;
        accumulated=elapsed(now); paused=true; anchor=null;
    }
    public boolean resume(long now) {
        if (!active || !paused || !fresh(now)) return false;
        paused=false; started=now; anchor=last; return true;
    }
    public void stop(long now) {
        if (!active) return;
        accumulated=elapsed(now); active=false; paused=false; anchor=null;
    }
    public void unavailable(String message) {
        if (active && !paused) incomplete=true;
        last=null; anchor=null; liveKmh=Double.NaN; status=message;
    }
    public void tick(long now) {
        if (last != null && !fresh(now)) unavailable("Mất tín hiệu GPS — đang chờ lại");
    }
    public boolean accept(Fix f, long now) {
        if (f.time <= lastInput) return false; // duplicate / out of order
        if (f.time > now || now-f.time>FRESH_MS) return false;
        lastInput=f.time;
        if (!Double.isFinite(f.lat) || !Double.isFinite(f.lon) || Math.abs(f.lat)>90 || Math.abs(f.lon)>180
                || !Double.isFinite(f.accuracy) || f.accuracy<=0 || f.accuracy>MAX_ACCURACY) {
            unavailable("GPS yếu — hãy ra nơi thoáng"); return false;
        }
        Fix previous=last;
        if (previous != null && f.time-previous.time>GAP_MS) {
            if (active && !paused) incomplete=true;
            anchor=null; previous=null;
        }
        double dt=previous==null ? 0 : (f.time-previous.time)/1000.0;
        double step=previous==null ? 0 : distance(previous,f);
        boolean reliableSpeed=f.hasSpeed && Double.isFinite(f.speed) && f.speed>=0
            && (!f.hasSpeedAccuracy || (Double.isFinite(f.speedAccuracy) && f.speedAccuracy>=0 && f.speedAccuracy<=3));
        if ((reliableSpeed && f.speed>MAX_SPEED) || (dt>0 && step>MAX_SPEED*dt+previous.accuracy+f.accuracy)) {
            unavailable("Bỏ qua điểm GPS nhảy bất thường"); return false;
        }
        if (reliableSpeed) liveKmh=f.speed*3.6;
        else if (dt>0 && step>=Math.max(3, Math.max(previous.accuracy,f.accuracy))) liveKmh=step/dt*3.6;
        else liveKmh=Double.NaN;
        if (active && !paused) {
            if (Double.isFinite(liveKmh)) maxKmh=Math.max(maxKmh,liveKmh);
            if (anchor != null) {
                double d=distance(anchor,f);
                // Suppress stationary drift; retain anchor to accumulate small walking steps.
                if (reliableSpeed && f.speed<0.5) anchor=f;
                else if (d>=Math.max(2, Math.min(anchor.accuracy,f.accuracy)*0.5)) { meters+=d; anchor=f; }
            } else anchor=f;
        }
        last=f;
        status="GPS tốt · sai số vị trí khoảng ±"+Math.round(f.accuracy)+" m";
        return true;
    }
    public static double distance(Fix a, Fix b) {
        double dLat=Math.toRadians(b.lat-a.lat), dLon=Math.toRadians(b.lon-a.lon);
        double h=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(a.lat))*Math.cos(Math.toRadians(b.lat))*Math.sin(dLon/2)*Math.sin(dLon/2);
        return 6371008.8*2*Math.atan2(Math.sqrt(Math.min(1,h)),Math.sqrt(Math.max(0,1-h)));
    }
}
