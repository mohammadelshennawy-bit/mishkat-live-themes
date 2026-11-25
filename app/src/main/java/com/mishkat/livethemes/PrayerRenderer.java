package com.mishkat.livethemes;
import android.content.Context;
import android.graphics.*;
import java.util.*;

/** Offline approximate Egyptian Survey calculation; civil timezone Africa/Cairo. */
final class PrayerRenderer {
 static final String[] CITIES={"القاهرة","شبين الكوم","الإسكندرية","المنصورة","أسيوط","أسوان"};
 static final double[] LAT={30.0444,30.5586,31.2001,31.04,27.18,24.09}, LON={31.2357,31.01,29.9187,31.38,31.18,32.90};
 static final String[] METHODS={"الهيئة المصرية العامة للمساحة","رابطة العالم الإسلامي","أم القرى","أمريكا الشمالية ISNA","كراتشي"};
 static final String[] N={"الفجر","الشروق","الظهر","العصر","المغرب","العشاء"};
 static final String[] ICON={"☀","☀","◷","☀","◐","☾"};
 static double rad(double a){return Math.toRadians(a);}
 static double angle(double lat,double dec,double alt){return Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,(Math.sin(rad(alt))-Math.sin(lat)*Math.sin(dec))/(Math.cos(lat)*Math.cos(dec))))))*4;}
 static long[] times(Calendar day,double lat,double lon,int method,boolean ramadan){
  double g=2*Math.PI/365*(day.get(Calendar.DAY_OF_YEAR)-1);
  double eq=229.18*(.000075+.001868*Math.cos(g)-.032077*Math.sin(g)-.014615*Math.cos(2*g)-.040849*Math.sin(2*g));
  double d=.006918-.399912*Math.cos(g)+.070257*Math.sin(g)-.006758*Math.cos(2*g)+.000907*Math.sin(2*g)-.002697*Math.cos(3*g)+.00148*Math.sin(3*g);
  double l=rad(lat), noon=720-4*lon-eq+day.getTimeZone().getOffset(day.getTimeInMillis())/60000.0;
  double asr=Math.toDegrees(Math.atan(1/(1+Math.tan(Math.abs(l-d)))));
  double[] fa={19.5,18,18.5,15,18}, ia={17.5,17,0,15,18};
  double[] m={noon-angle(l,d,-fa[method]),noon-angle(l,d,-.833),noon,noon+angle(l,d,asr),noon+angle(l,d,-.833),method==2?noon+angle(l,d,-.833)+(ramadan?120:90):noon+angle(l,d,-ia[method])};
  Calendar start=(Calendar)day.clone();start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);
  long[] out=new long[6];for(int i=0;i<6;i++)out[i]=start.getTimeInMillis()+Math.round(m[i])*60000;return out;
 }
 static String time(long t, TimeZone zone){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("h:mm",Locale.US);f.setTimeZone(zone);Calendar c=Calendar.getInstance(f.getTimeZone());c.setTimeInMillis(t);return f.format(new Date(t))+(c.get(Calendar.HOUR_OF_DAY)<12?" ص":" م");}
 static void rect(Canvas c,Paint p,float x,float y,float w,float h,int color){p.setColor(color);c.drawRoundRect(x,y,x+w,y+h,15,15,p);}
 static void txt(Canvas c,Paint p,String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,p);}
 static void draw(Context ctx,Canvas c,int w,int h,ClockRenderer.Style st,Calendar ignored){
  int city=Math.max(0,Math.min(st.city,CITIES.length-1)); boolean gps=st.gps; double lat=gps?st.lat:LAT[city], lon=gps?st.lon:LON[city]; int method=Math.max(0,Math.min(st.method,METHODS.length-1)); String place=gps?"موقعي":CITIES[city]; TimeZone zone=gps?TimeZone.getDefault():TimeZone.getTimeZone("Africa/Cairo"); Calendar now=Calendar.getInstance(zone);long ms=now.getTimeInMillis();long[] t=times(now,lat,lon,method,st.ramadan);int next=0;long target=0;for(int i=0;i<6;i++){if(i!=1&&t[i]>ms){next=i;target=t[i];break;}}boolean tomorrow=target==0;if(tomorrow){now.add(Calendar.DATE,1);target=times(now,lat,lon,method,st.ramadan)[0];}
  long sec=Math.max(0,(target-ms+999)/1000);String countdown=String.format(Locale.US,"%02d:%02d:%02d",sec/3600,sec/60%60,sec%60);
  c.save();float scale=Math.min(w/360f,h/430f);c.translate((w-360*scale)/2,(h-430*scale)/2);c.scale(scale,scale);Paint p=new Paint(3);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));int gold=0xffebce86,navy=0xff142653,cream=0xffffefd0;
  rect(c,p,8,8,344,414,0xf512234d);txt(c,p,"مواقيت الصلاة",180,43,24,gold);
  rect(c,p,20,58,320,100,0xff22345b);txt(c,p,"الصلاة القادمة • "+place,180,80,14,cream);txt(c,p,N[next]+"  "+time(target,zone),180,105,20,gold);txt(c,p,countdown,180,140,28,gold);
  txt(c,p,METHODS[method],180,181,13,cream);
  for(int i=0;i<6;i++){float x=20+(2-i%3)*108,y=193+(i/3)*84;boolean active=i==next&&!tomorrow;int fg=active?navy:gold;rect(c,p,x,y,104,78,active?0xffd4ad60:0xff20325b);txt(c,p,ICON[i],x+52,y+23,20,fg);txt(c,p,N[i],x+52,y+44,15,active?navy:cream);txt(c,p,time(t[i],zone),x+52,y+65,15,fg);}
  txt(c,p,place+" • حساب محلي تقريبي",180,388,13,cream);txt(c,p,"تُضبط المدينة من التطبيق",180,408,12,gold);c.restore();
 }
}
