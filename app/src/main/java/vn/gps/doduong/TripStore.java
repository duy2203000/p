package vn.gps.doduong;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

final class TripStore {
    private final SharedPreferences prefs;
    TripStore(Context context) { prefs=context.getSharedPreferences("trips",Context.MODE_PRIVATE); }
    JSONArray history() {
        try { return new JSONArray(prefs.getString("history","[]")); }
        catch (Exception e) { return new JSONArray(); }
    }
    void append(JSONObject item) {
        JSONArray old=history(), next=new JSONArray(); next.put(item);
        for(int i=0; i<old.length() && next.length()<100; i++) {
            JSONObject row=old.optJSONObject(i);
            if(row!=null && row.optLong("id")!=item.optLong("id")) next.put(row);
        }
        prefs.edit().putString("history",next.toString()).remove("pending").apply();
    }
    void checkpoint(JSONObject item) { prefs.edit().putString("pending",item.toString()).apply(); }
    void recover() {
        String value=prefs.getString("pending",null);
        if(value==null) return;
        try {
            JSONObject item=new JSONObject(value);
            item.put("incomplete",true); item.put("interrupted",true); append(item);
        } catch(Exception e) { prefs.edit().remove("pending").apply(); }
    }
    void clear() { prefs.edit().remove("history").apply(); }
    static JSONObject snapshot(TripEngine engine,long now,long id,String label) {
        JSONObject o=new JSONObject();
        try {
            o.put("id",id); o.put("name",label); o.put("meters",engine.meters);
            o.put("durationMs",engine.elapsed(now)); o.put("averageKmh",engine.average(now));
            o.put("maxKmh",engine.maxKmh); o.put("incomplete",engine.incomplete);
        } catch(Exception ignored) { }
        return o;
    }
}
