package vn.gps.doduong;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BG=0xff0b1420, CARD=0xff152232, GREEN=0xff43e4ae, TEXT=0xfff1f5fa, MUTED=0xffa6b8ca;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private GpsService service;
    private boolean bound;
    private LinearLayout root;
    private TextView speed,status,distance,time,average,max,phase,warning;
    private EditText name;
    private Button gps,start,pause,finish;
    private String pendingCsv;
    private final ServiceConnection connection=new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName n,IBinder b) {
            service=((GpsService.LocalBinder)b).service(); render();
        }
        @Override public void onServiceDisconnected(ComponentName n) { service=null; render(); }
    };
    private final Runnable refresh=new Runnable() {
        @Override public void run() { render(); handler.postDelayed(this,500); }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(state!=null) pendingCsv=state.getString("pendingCsv");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildUi();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putString("pendingCsv",pendingCsv);
    }
    @Override protected void onStart() {
        super.onStart();
        if(!bound) bound=bindService(new Intent(this,GpsService.class),connection,0);
        handler.post(refresh);
    }
    @Override protected void onStop() {
        handler.removeCallbacks(refresh);
        boolean stop=service!=null && !service.engine.active && !isChangingConfigurations();
        if(bound) { unbindService(connection); bound=false; }
        service=null;
        if(stop) stopService(new Intent(this,GpsService.class));
        super.onStop();
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String content,int size,int color) {
        TextView t=new TextView(this); t.setText(content); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(0,dp(4),0,dp(4)); return t;
    }
    private LinearLayout panel() {
        LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(18),dp(14),dp(18),dp(14)); p.setBackground(shape(CARD,22));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(12),0,0);
        root.addView(p,lp); return p;
    }
    private Button button(String label,boolean primary) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(16);
        b.setTextColor(primary?BG:TEXT); b.setBackground(shape(primary?GREEN:0xff25364b,14));
        b.setMinHeight(dp(54)); b.setPadding(dp(10),dp(12),dp(10),dp(12));
        return b;
    }
    private void addButton(LinearLayout container,Button b) {
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.topMargin=dp(10); container.addView(b,lp);
    }
    private TextView metric(LinearLayout parent,String label) {
        LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text(label,15,MUTED); row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView value=text("—",21,TEXT); value.setTypeface(null,Typeface.BOLD); row.addView(value);
        parent.addView(row,new LinearLayout.LayoutParams(-1,-2)); return value;
    }
    private void buildUi() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(18),dp(20),dp(24));
        scroll.addView(root); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)-> {
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets();
        });
        TextView title=text("GPS ĐO ĐƯỜNG",24,TEXT); title.setTypeface(null,Typeface.BOLD); root.addView(title);
        root.addView(text("Chọn điểm đầu. Đo đến điểm cuối.",15,MUTED));
        LinearLayout dial=panel();
        phase=text("SẴN SÀNG ĐO",13,GREEN); phase.setGravity(Gravity.CENTER); dial.addView(phase);
        speed=text("—",82,TEXT); speed.setGravity(Gravity.CENTER); speed.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));
        speed.setContentDescription("Tốc độ hiện tại"); dial.addView(speed);
        TextView unit=text("km/h · tốc độ hiện tại",15,MUTED); unit.setGravity(Gravity.CENTER); dial.addView(unit);
        status=text("Bấm Bật GPS để chuẩn bị đo",14,MUTED); status.setGravity(Gravity.CENTER); dial.addView(status);
        LinearLayout stats=panel();
        distance=metric(stats,"Quãng đường"); time=metric(stats,"Thời gian đo");
        average=metric(stats,"Tốc độ trung bình"); max=metric(stats,"Tốc độ cao nhất");
        warning=text("",13,0xffffca80); stats.addView(warning);
        name=new EditText(this); name.setSingleLine(true); name.setTextSize(16); name.setTextColor(TEXT);
        name.setHintTextColor(MUTED); name.setHint("Tên đoạn đường (không bắt buộc)");
        name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});
        root.addView(name,new LinearLayout.LayoutParams(-1,dp(60)));
        gps=button("Bật GPS",false); addButton(root,gps); gps.setOnClickListener(v->ensureGps());
        start=button("Bắt đầu đoạn đường",true); addButton(root,start);
        start.setOnClickListener(v-> {
            if(service==null) { ensureGps(); return; }
            if(!service.startTrip(name.getText().toString())) toast("Chờ GPS tốt rồi bấm Bắt đầu.");
            render();
        });
        LinearLayout actions=new LinearLayout(this);
        LinearLayout.LayoutParams rowLp=new LinearLayout.LayoutParams(-1,-2); rowLp.topMargin=dp(10); root.addView(actions,rowLp);
        pause=button("Tạm dừng",false); finish=button("Kết thúc",false);
        LinearLayout.LayoutParams half=new LinearLayout.LayoutParams(0,-2,1); half.rightMargin=dp(5); actions.addView(pause,half);
        LinearLayout.LayoutParams other=new LinearLayout.LayoutParams(0,-2,1); other.leftMargin=dp(5); actions.addView(finish,other);
        pause.setOnClickListener(v-> {
            if(service==null) return;
            if(service.engine.paused) { if(!service.resumeTrip()) toast("Chờ GPS tốt để tiếp tục."); }
            else service.pauseTrip(); render();
        });
        finish.setOnClickListener(v-> {
            if(service==null || !service.engine.active) return;
            new AlertDialog.Builder(this).setTitle("Kết thúc đoạn đường?").setMessage("Lưu quãng đường và các chỉ số tốc độ đến thời điểm bạn bấm Lưu kết quả.")
                .setNegativeButton("Đo tiếp",null).setPositiveButton("Lưu kết quả",(d,w)-> {
                    if(service!=null) { service.finishTrip(); render(); toast("Đã lưu trong Lịch sử."); }
                }).show();
        });
        Button history=button("Lịch sử các đoạn đã đo",false); addButton(root,history); history.setOnClickListener(v->showHistory());
        TextView note=text("Đo đường thực tế đã đi, không phải khoảng cách đường chim bay giữa hai đầu. GPS có sai số; nên đo ngoài trời. Khi tạm dừng, thời gian và quãng đường không được cộng.",13,MUTED);
        note.setPadding(0,dp(16),0,dp(8)); root.addView(note);
        Button help=button("Cách sử dụng",false); addButton(root,help);
        help.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Đo một đoạn đường")
            .setMessage("1. Bật Vị trí của điện thoại và bấm Bật GPS. Cấp quyền Vị trí chính xác khi dùng ứng dụng.\n\n2. Ra nơi thoáng và chờ GPS tốt. Tại điểm đầu, bấm Bắt đầu đoạn đường.\n\n3. Di chuyển đến điểm cuối, bấm Kết thúc → Lưu kết quả.\n\nTốc độ trung bình = quãng đường / thời gian đo, có tính lúc đứng yên, không tính lúc tạm dừng.\n\nNếu mất GPS, ứng dụng không nối tắt qua khoảng mất tín hiệu và đánh dấu kết quả chưa đầy đủ.\n\nKhi đang đo, ứng dụng có thông báo và tiếp tục nhận GPS khi tắt màn hình. Một số điện thoại tiết kiệm pin có thể ngắt ứng dụng.\n\nLưu tối đa 100 đoạn trên máy, không gửi vị trí lên mạng. Xuất CSV trong Lịch sử. Hãy thao tác khi đã dừng xe an toàn.")
            .setPositiveButton("Đã hiểu",null).show());
        render();
    }
    private void enable(Button b,boolean enabled) { b.setEnabled(enabled); b.setAlpha(enabled?1f:0.4f); }
    private void render() {
        if(speed==null) return;
        TripEngine e=service==null?null:service.engine;
        long now=SystemClock.elapsedRealtime();
        if(e!=null) e.tick(now);
        boolean active=e!=null && e.active, fresh=e!=null && e.fresh(now);
        speed.setText(fresh && Double.isFinite(e.liveKmh)?format(e.liveKmh,1):"—");
        status.setText(e==null?"Bấm Bật GPS để chuẩn bị đo":e.status);
        phase.setText(active?(e.paused?"ĐÃ TẠM DỪNG":"ĐANG ĐO ĐOẠN ĐƯỜNG"):(e!=null && e.elapsed(now)>0?"KẾT QUẢ ĐOẠN VỪA ĐO":"SẴN SÀNG ĐO"));
        distance.setText(e==null?"0 m":meters(e.meters));
        time.setText(duration(e==null?0:e.elapsed(now)));
        average.setText(format(e==null?0:e.average(now),1)+" km/h");
        max.setText(format(e==null?0:e.maxKmh,1)+" km/h");
        warning.setVisibility(e!=null && e.incomplete?View.VISIBLE:View.GONE);
        warning.setText("Có gián đoạn GPS: quãng đường và tốc độ trung bình có thể bị thiếu.");
        name.setEnabled(!active);
        if(active && !name.getText().toString().equals(service.tripName())) name.setText(service.tripName());
        gps.setText(e==null?"Bật GPS":"Cài đặt Vị trí / quyền GPS");
        enable(start,!active && fresh); enable(pause,active); enable(finish,active);
        pause.setText(active && e.paused?"Tiếp tục":"Tạm dừng");
    }
    private void ensureGps() {
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},10); return;
        }
        if(service!=null) { startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); return; }
        startGps();
    }
    private void startGps() {
        try {
            Intent i=new Intent(this,GpsService.class); startForegroundService(i);
            if(!bound) bound=bindService(i,connection,Context.BIND_AUTO_CREATE);
            if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},11);
        } catch(RuntimeException e) { toast("Không bật được GPS. Hãy mở lại ứng dụng và kiểm tra quyền Vị trí."); }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request!=10) return;
        if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED) startGps();
        else new AlertDialog.Builder(this).setTitle("Cần vị trí chính xác")
            .setMessage("Đo tốc độ cần quyền Vị trí chính xác. Trong cài đặt ứng dụng, chọn Quyền → Vị trí → Cho phép khi dùng ứng dụng, rồi bật Vị trí chính xác.")
            .setNegativeButton("Để sau",null).setPositiveButton("Mở cài đặt",(d,w)->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).show();
    }
    private void showHistory() {
        JSONArray rows=new TripStore(this).history();
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(10),dp(20),dp(10));
        ScrollView scroll=new ScrollView(this); scroll.addView(box);
        if(rows.length()==0) box.addView(text("Chưa có đoạn đường đã lưu.",16,TEXT));
        for(int i=0;i<rows.length();i++) {
            JSONObject o=rows.optJSONObject(i); if(o==null) continue;
            String label=o.optString("name")+"\n"+new SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(new Date(o.optLong("id")));
            TextView heading=text(label,17,GREEN); heading.setTypeface(null,Typeface.BOLD); box.addView(heading);
            String detail=meters(o.optDouble("meters"))+" · "+duration(o.optLong("durationMs"))
                +"\nTrung bình: "+format(o.optDouble("averageKmh"),1)+" km/h"
                +"\nCao nhất: "+format(o.optDouble("maxKmh"),1)+" km/h"
                +(o.optBoolean("incomplete")?"\n⚠ Kết quả chưa đầy đủ / GPS gián đoạn":"")
                +(o.optBoolean("interrupted")?"\nỨng dụng bị ngắt; khôi phục lần lưu gần nhất":"");
            TextView details=text(detail,15,TEXT); details.setPadding(0,0,0,dp(20)); box.addView(details);
        }
        new AlertDialog.Builder(this).setTitle("Lịch sử · "+rows.length()+"/100 đoạn").setView(scroll)
            .setPositiveButton("Đóng",null)
            .setNeutralButton("Xuất CSV",(d,w)-> {
                if(rows.length()==0) { toast("Chưa có dữ liệu để xuất."); return; }
                pendingCsv=csv(rows);
                Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/csv")
                    .putExtra(Intent.EXTRA_TITLE,"GPS-DoDuong-"+new SimpleDateFormat("yyyyMMdd-HHmm",Locale.US).format(new Date())+".csv");
                startActivityForResult(i,20);
            })
            .setNegativeButton("Xóa lịch sử",(d,w)->new AlertDialog.Builder(this).setTitle("Xóa tất cả lịch sử?")
                .setMessage("Các kết quả đã lưu sẽ bị xóa khỏi điện thoại.").setNegativeButton("Hủy",null)
                .setPositiveButton("Xóa",(d2,w2)-> {new TripStore(this).clear(); toast("Đã xóa lịch sử.");}).show()).show();
    }
    private static String cell(String value) {
        if(value.matches("^[=+@\\-].*")) value="'"+value;
        return "\""+value.replace("\"","\"\"")+"\"";
    }
    private static String csv(JSONArray rows) {
        StringBuilder b=new StringBuilder("\ufeffTên đoạn,Thời điểm bắt đầu,Quãng đường (m),Thời gian đo (s),Trung bình (km/h),Cao nhất (km/h),Chưa đầy đủ\r\n");
        for(int i=0;i<rows.length();i++) {
            JSONObject o=rows.optJSONObject(i); if(o==null) continue;
            b.append(cell(o.optString("name"))).append(',')
                .append(cell(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss XXX",Locale.US).format(new Date(o.optLong("id"))))).append(',')
                .append(String.format(Locale.US,"%.2f,%.1f,%.2f,%.2f,",o.optDouble("meters"),o.optLong("durationMs")/1000.0,o.optDouble("averageKmh"),o.optDouble("maxKmh")))
                .append(o.optBoolean("incomplete")?"Có":"Không").append("\r\n");
        }
        return b.toString();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=20) return;
        if(result==RESULT_OK && data!=null && data.getData()!=null && pendingCsv!=null) {
            try(OutputStream out=getContentResolver().openOutputStream(data.getData())) {
                if(out==null) throw new java.io.IOException("No output stream");
                out.write(pendingCsv.getBytes(StandardCharsets.UTF_8)); toast("Đã xuất CSV.");
            } catch(Exception e) { toast("Không lưu được CSV. Hãy chọn vị trí khác."); }
        }
        pendingCsv=null;
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    private static String format(double value,int places) { return String.format(Locale.forLanguageTag("vi-VN"),"%."+places+"f",value); }
    private static String meters(double value) { return value<1000?format(value,0)+" m":format(value/1000,3)+" km"; }
    private static String duration(long ms) {
        long s=ms/1000; return String.format(Locale.US,"%02d:%02d:%02d",s/3600,(s%3600)/60,s%60);
    }
}
