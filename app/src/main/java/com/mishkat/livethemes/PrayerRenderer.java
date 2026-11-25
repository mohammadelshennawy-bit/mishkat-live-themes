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
 static void txt(Canvas c,Paint p,String s,float x,float y,float size,int color){p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);p.setShadowLayer(1.5f,0,1, color==0xff111827?0x99ffffff:0xcc000000);c.drawText(s,x,y,p);p.clearShadowLayer();}
 static void draw(Context ctx,Canvas c,int w,int h,ClockRenderer.Style st,Calendar ignored,int level,boolean charging){
  int city=Math.max(0,Math.min(st.city,CITIES.length-1)); boolean gps=st.gps; double lat=gps?st.lat:LAT[city], lon=gps?st.lon:LON[city]; int method=Math.max(0,Math.min(st.method,METHODS.length-1)); String place=gps?"موقعي":CITIES[city]; TimeZone zone=gps?TimeZone.getDefault():TimeZone.getTimeZone("Africa/Cairo"); Calendar now=Calendar.getInstance(zone);long ms=now.getTimeInMillis();long[] t=times(now,lat,lon,method,st.ramadan);int next=0;long target=0;for(int i=0;i<6;i++){if(i!=1&&t[i]>ms){next=i;target=t[i];break;}}boolean tomorrow=target==0;if(tomorrow){now.add(Calendar.DATE,1);target=times(now,lat,lon,method,st.ramadan)[0];}
  long sec=Math.max(0,(target-ms+999)/1000);String countdown=String.format(Locale.US,"%02d:%02d:%02d",sec/3600,sec/60%60,sec%60);
  int[] colors={0xff142653,0xff000000,0xff07503c,0xff492b68,0xff583921,0xffffedcf};
  int idx=Math.max(0,Math.min(st.prayerColor,colors.length-1));
  int alpha=Math.round(Math.max(0,Math.min(100,st.prayerOpacity))*2.55f);
  int base=(colors[idx]&0xffffff)|(alpha<<24);
  int gold=0xffebce86, navy=0xff142653;
  int fg=st.prayerText==1?gold:st.prayerText==2?0xffffffff:st.prayerText==3?0xff111827:idx==5?0xff111827:gold;
  int cream=st.prayerText==0&&idx!=5?0xffffefd0:fg;
  int tile=(colors[idx]&0xffffff)|(Math.round(alpha*.35f)<<24);
  int activeColor=(0xd4ad60)|(alpha<<24);
  // Separate objects, with a reserved top quarter and bottom shortcut area.
  float panelH=326, batteryH=st.prayerBattery?34:0, dateH=(st.prayerGregorian||st.prayerHijri)?(st.prayerGregorian&&st.prayerHijri?54:32):0;
  float verseH=st.verse?46:0;
  float total=panelH+batteryH+dateH+verseH+12;
  float scale=Math.min(w/360f*Math.max(.6f,Math.min(1f,st.prayerScale/100f)),h*.64f/total);
  float minTop=h*.27f, maxTop=Math.max(minTop,h*.91f-total*scale);
  float top=Math.max(minTop,Math.min(maxTop,h*st.prayerPosition/100f-panelH*scale/2));
  c.save();c.translate((w-360*scale)/2,top);c.scale(scale,scale);Paint p=new Paint(3);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
  rect(c,p,3,0,354,panelH,base);txt(c,p,"مواقيت الصلاة",180,29,25,fg);
  rect(c,p,10,39,340,84,tile);txt(c,p,"الصلاة القادمة • "+place,180,58,15,cream);txt(c,p,N[next]+"  "+time(target,zone),180,80,21,fg);
  p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));txt(c,p,countdown,180,112,32,fg);
  txt(c,p,METHODS[method],180,145,15,cream);
  for(int i=0;i<6;i++){float x=9+(2-i%3)*115,y=155+(i/3)*84;boolean active=i==next&&!tomorrow;int ink=active&&alpha>=180?navy:fg;rect(c,p,x,y,112,80,active?activeColor:tile);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(active?2:0.7f);rect(c,p,x,y,112,80,active?gold:0x66999999);p.setStyle(Paint.Style.FILL);txt(c,p,ICON[i],x+56,y+22,20,ink);txt(c,p,N[i],x+56,y+45,18,ink);txt(c,p,time(t[i],zone),x+56,y+68,18,ink);}
  float y=panelH+6;
  if(st.prayerBattery){rect(c,p,112,y,136,28,base);int pct=Math.max(0,Math.min(100,level));p.setColor(fg);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.5f);c.drawRoundRect(126,y+7,168,y+21,3,3,p);p.setStyle(Paint.Style.FILL);c.drawRect(169,y+11,172,y+17,p);c.drawRect(129,y+10,129+36*pct/100f,y+18,p);txt(c,p,pct+"%"+(charging?" ⚡":""),207,y+20,18,fg);y+=34;}
  if(dateH>0){rect(c,p,43,y,274,dateH-4,base);y+=20;}
  Calendar today=Calendar.getInstance(zone);
  if(st.prayerGregorian){java.text.SimpleDateFormat f=new java.text.SimpleDateFormat("EEEE، d MMMM yyyy",new Locale("ar","EG"));f.setTimeZone(zone);txt(c,p,f.format(today.getTime()).replace('٠','0').replace('١','1').replace('٢','2').replace('٣','3').replace('٤','4').replace('٥','5').replace('٦','6').replace('٧','7').replace('٨','8').replace('٩','9'),180,y,17,cream);y+=23;}
  if(st.prayerHijri){android.icu.util.IslamicCalendar hc=new android.icu.util.IslamicCalendar();hc.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);hc.setTimeZone(android.icu.util.TimeZone.getTimeZone(zone.getID()));hc.setTimeInMillis(ms);String[] months={"محرم","صفر","ربيع الأول","ربيع الآخر","جمادى الأولى","جمادى الآخرة","رجب","شعبان","رمضان","شوال","ذو القعدة","ذو الحجة"};txt(c,p,hc.get(android.icu.util.Calendar.DAY_OF_MONTH)+" "+months[hc.get(android.icu.util.Calendar.MONTH)]+" "+hc.get(android.icu.util.Calendar.YEAR)+" هـ",180,y,17,fg);}
  if(st.verse){float verseTop=Math.max(panelH+batteryH+dateH+12,(h*.85f-top)/scale);verseTop=Math.min(verseTop,(h*.91f-top)/scale-42);Paint vp=ClockRenderer.text(ClockRenderer.font(ctx),25,fg);ClockRenderer.rtl(c,ClockRenderer.VERSE,180,verseTop,360,vp);}
  c.restore();
 }
}
