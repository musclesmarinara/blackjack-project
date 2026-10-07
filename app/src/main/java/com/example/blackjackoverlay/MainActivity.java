package com.example.blackjackoverlay;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.projection.*;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import android.graphics.Color;
import java.io.*;

public final class MainActivity extends Activity {
    private String exportPath;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);if(state!=null)exportPath=state.getString("exportPath");
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(30,50,30,30);body.setBackgroundColor(Color.rgb(13,24,33));
        TextView title=new TextView(this);title.setText("Blackjack Project v.01\nExperimental test build");title.setTextSize(27);title.setTextColor(0xff78ebbb);body.addView(title);
        TextView info=new TextView(this);info.setText("Watch a seat without placing bets. This app reads visible cards and displays a provisional count and strategy suggestion. Recognition is not validated.\n\n1. Choose decks and start capture.\n2. Open the table and tap Calibrate. Apply Portrait preset. Use ONE table box covering the physical dealer and every player hand, leaving digital cards, shoes and chips outside. Move each dealer/seat anchor to its first-card position, including empty seats. Set the expected card angle with ±5°. Before any cards are dealt, tap Learn empty with all hands off the table, then Save calibration. Reopen Calibrate when cards appear to use Test frame; cyan boxes show the regions sent to OCR. Learn empty again each capture session or after any layout/camera change. Press Start in the overlay: green RUNNING means continuous scanning is enabled. Keep the overlay below the cards. The app can detect a new hand from cards when the game status is covered. Stop turns it red and stops analysis/recording. Close ends screen capture.\n3. Tap Seat to choose 1 (far left), 2 (second from left), through 7 (far right). Wait for a new shoe.\n4. Tap Record in the overlay. Watch a few hands, tap Record again, then Close. Return here to export the clip and log.\n\nUse the built-in recorder, not Samsung's screen recorder: a second capture can stop this app. Recording has no audio. Keep orientation and zoom fixed.\n\nUnknown burn quantity requires review. Count advice is enabled in Review and only used after a known shoe, confirmed burn quantity, and cleared uncertainty. Unconfirmed table rules stay assumptions. Separated card clusters can be selected with Hand. Touching split hands or ambiguous ownership require review; automatic reconstruction is not guaranteed.\n\nScreen capture stays on this phone. No login, network permission, bets, taps into the game, or account access. Count resets when capture ends.");info.setTextColor(Color.WHITE);info.setTextSize(15);body.addView(info);
        Spinner decks=new Spinner(this);decks.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"8 decks · Stake table","6 decks"}));decks.setSelection(getPreferences().getInt("decks",8)==6?1:0);body.addView(decks);
        add(body,"Start observation",()->{getPreferences().edit().putInt("decks",decks.getSelectedItemPosition()==0?8:6).apply();begin();});
        add(body,"Stop capture",()->stopService(new Intent(this,CaptureService.class)));
        add(body,"Export last recording",()->export("lastClip","video/mp4","Blackjack-observation.mp4"));
        add(body,"Export last diagnostic log",()->export("lastLog","application/x-ndjson","Blackjack-diagnostics.jsonl"));
        add(body,"Delete local test recordings and logs",()->new AlertDialog.Builder(this).setTitle("Delete local test files?").setMessage("Export anything you want to keep first.").setPositiveButton("Delete",(d,w)->{if(CaptureService.active){toast("Stop capture first");return;}File dir=new File(getFilesDir(),"sessions");File[] files=dir.listFiles();if(files!=null)for(File f:files)f.delete();getPreferences().edit().remove("lastClip").remove("lastLog").apply();toast("Local test files deleted");}).setNegativeButton("Cancel",null).show());
        ScrollView scroll=new ScrollView(this);scroll.addView(body);setContentView(scroll);
    }
    private SharedPreferences getPreferences(){return getSharedPreferences("config",0);}
    private void add(LinearLayout body,String text,Runnable run){Button b=new Button(this);b.setText(text);body.addView(b);b.setOnClickListener(v->run.run());}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private void export(String key,String mime,String name){
        if(CaptureService.active){toast("Stop capture before exporting");return;}
        exportPath=getPreferences().getString(key,null);if(exportPath==null||!new File(exportPath).isFile()){toast("No saved file yet");return;}
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(mime).putExtra(Intent.EXTRA_TITLE,name),20);
    }
    private void begin(){
        if(CaptureService.active){toast("Capture is already running");return;}
        if(!Settings.canDrawOverlays(this)){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));toast("Allow appear on top, then return and tap Start");return;}
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},3);return;}
        requestCapture();
    }
    private void requestCapture(){MediaProjectionManager m=getSystemService(MediaProjectionManager.class);startActivityForResult(Build.VERSION.SDK_INT>=34?m.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay()):m.createScreenCaptureIntent(),10);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==3)requestCapture();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("exportPath",exportPath);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null)return;
        if(request==10){startForegroundService(new Intent(this,CaptureService.class).putExtra("code",result).putExtra("data",data));moveTaskToBack(true);}
        if(request==20&&exportPath!=null){final Uri uri=data.getData();final String path=exportPath;new Thread(()->{try(InputStream in=new FileInputStream(path);OutputStream out=getContentResolver().openOutputStream(uri)){byte[] b=new byte[65536];int n;while((n=in.read(b))>=0)out.write(b,0,n);runOnUiThread(()->toast("Exported"));}catch(Exception e){runOnUiThread(()->toast("Export failed: "+e.getMessage()));}}).start();}
    }
}
