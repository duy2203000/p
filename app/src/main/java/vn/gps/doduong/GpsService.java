package vn.gps.doduong;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.*;
import android.os.*;
import java.util.Locale;

public final class GpsService extends Service implements LocationListener {
    public final TripEngine engine=new TripEngine();
    private final Binder binder=new LocalBinder();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LocationManager manager;
    private TripStore store;
    private boolean listening, foreground;
    private long tripId, lastCheckpoint;
    private String tripName="Đoạn đường";
    public final class LocalBinder extends Binder { public GpsService service() { return GpsService.this; } }
    private final Runnable ticker=new Runnable() {
        @Override public void run() {
            long now=SystemClock.elapsedRealtime(); engine.tick(now);
            if(engine.active && now-lastCheckpoint>=5000) { checkpoint(); lastCheckpoint=now; }
            if(foreground) ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1,notification());
            handler.postDelayed(this,1000);
        }
    };
    @Override public void onCreate() {
        super.onCreate(); store=new TripStore(this); store.recover();
        manager=(LocationManager)getSystemService(LOCATION_SERVICE);
        NotificationChannel channel=new NotificationChannel("gps","Đo tốc độ GPS",NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        handler.post(ticker);
    }
    @Override public IBinder onBind(Intent intent) { return binder; }
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            engine.unavailable("Cần cấp quyền vị trí chính xác"); stopSelf(); return START_NOT_STICKY;
        }
        try {
            if(Build.VERSION.SDK_INT>=29) startForeground(1,notification(),ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            else startForeground(1,notification());
            foreground=true;
            if(!listening) {
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this,Looper.getMainLooper());
                listening=true;
            }
            if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) engine.unavailable("GPS đang tắt — bật Vị trí trên điện thoại");
        } catch(SecurityException | IllegalArgumentException e) {
            engine.unavailable("Không thể bật GPS — kiểm tra quyền Vị trí"); stopSelf();
        }
        return START_NOT_STICKY;
    }
    public boolean startTrip(String name) {
        if(!engine.start(SystemClock.elapsedRealtime())) return false;
        tripId=System.currentTimeMillis(); tripName=name.trim().isEmpty()?"Đoạn đường":name.trim();
        checkpoint(); return true;
    }
    public void pauseTrip() { engine.pause(SystemClock.elapsedRealtime()); checkpoint(); }
    public boolean resumeTrip() {
        boolean ok=engine.resume(SystemClock.elapsedRealtime()); if(ok) checkpoint(); return ok;
    }
    public void finishTrip() {
        if(!engine.active) return;
        engine.tick(SystemClock.elapsedRealtime()); engine.stop(SystemClock.elapsedRealtime());
        store.append(TripStore.snapshot(engine,SystemClock.elapsedRealtime(),tripId,tripName));
    }
    public String tripName() { return tripName; }
    private void checkpoint() { if(engine.active) store.checkpoint(TripStore.snapshot(engine,SystemClock.elapsedRealtime(),tripId,tripName)); }
    private Notification notification() {
        String speed=engine.fresh(SystemClock.elapsedRealtime()) && Double.isFinite(engine.liveKmh)
            ? String.format(Locale.US,"%.1f km/h",engine.liveKmh) : "Đang chờ GPS";
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,"gps").setSmallIcon(R.drawable.ic_speed)
            .setContentTitle(engine.active ? (engine.paused?"Đã tạm dừng đo":"Đang đo · "+tripName) : "GPS Đo Đường")
            .setContentText(speed+" · "+String.format(Locale.US,"%.3f km",engine.meters/1000))
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_SERVICE).build();
    }
    @Override public void onLocationChanged(Location location) {
        if(!location.hasAccuracy()) { engine.unavailable("GPS chưa có độ chính xác"); return; }
        TripEngine.Fix fix=new TripEngine.Fix(location.getLatitude(),location.getLongitude(),location.getAccuracy(),
            location.getElapsedRealtimeNanos()/1000000L,location.getSpeed(),location.hasSpeed(),
            location.getSpeedAccuracyMetersPerSecond(),location.hasSpeedAccuracy());
        engine.accept(fix,SystemClock.elapsedRealtime());
    }
    @Override public void onProviderDisabled(String provider) { engine.unavailable("GPS đang tắt — bật Vị trí trên điện thoại"); }
    @Override public void onProviderEnabled(String provider) { engine.status="Đang tìm tín hiệu GPS…"; }
    @Override public void onStatusChanged(String provider,int status,Bundle extras) { }
    @Override public void onDestroy() {
        if(engine.active) { engine.incomplete=true; finishTrip(); }
        handler.removeCallbacksAndMessages(null);
        if(manager!=null) manager.removeUpdates(this);
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy();
    }
}
