
// FPCALC libbi, poistettu jarri ja .so lib
//import com.geecko.fpcalc.FpCalc;
public void checkAudio(){


        System.loadLibrary("fpcalc");

        String fileName=(Environment.getExternalStorageDirectory()
        .getAbsolutePath()+File.separator+"biisi3.amr");
        if(BuildConfig.DEBUG) Log.d(TAG,"fileName:"+fileName);
        String length="50";
        String[]args={"-length",length,fileName};
        String result=FpCalc.fpCalc(args);
        if(BuildConfig.DEBUG) Log.d(TAG,"checkAudio, result:"+result);
        String duration=result.substring(9,result.indexOf("F"));
        String fingerprint=result.substring(result.lastIndexOf("FINGERPRINT=")+12);
        if(BuildConfig.DEBUG) Log.d(TAG,"DURATION:"+duration);
        if(BuildConfig.DEBUG) Log.d(TAG,"FINGERPRINT:"+fingerprint);
        String biisi="?client=FkPa1JhL2a&duration="+duration+"&fingerprint="+fingerprint;
       /* JSONObject biisi = new JSONObject();
       /* try {
            biisi.put("client", "FkPa1JhL2a");
            biisi.put("duration", "5");
            biisi.put("fingerprint", result);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        HTTPSender httpSender=new HTTPSender();
        httpSender.execute("https://api.acoustid.org/v2/lookup"+biisi);

        }
        String fileName=(Environment.getExternalStorageDirectory()
        .getAbsolutePath()+File.separator+"biisi3.amr");
public void recordAudio(){


final WavAudioRecorder recorder=WavAudioRecorder.getInstanse();
        recorder.setOutputFile(fileName);
        recorder.prepare();
        recorder.start();

        Handler handler=new Handler();
        handler.postDelayed(new Runnable(){
@Override
public void run(){
        recorder.stop();
        try{
        playAudio();
        }catch(IOException e){
        e.printStackTrace();
        }
        checkAudio();
        }
        },30000);

        }

public void playAudio()throws IOException
        {

        MediaPlayer mediaPlayer=new MediaPlayer();
        mediaPlayer.setDataSource(fileName);
        mediaPlayer.prepare();
        mediaPlayer.start();
        }