package ro.traveldevel.util;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class FileHelper
{
    public static String fileName = "log.txt";
    public static String path = "/data/data/ro.traveldevel.obdtriptracerapp/logs/" ;

    public static boolean debug(String TAG, String data){
        try
        {
            new File(path).mkdir();

            File file = new File(path + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fileOutputStream = new FileOutputStream(file,true);
            fileOutputStream.write((new Date().toString() + " - " + TAG + " : " + data + System.getProperty("line.separator")).getBytes());

            return true;
        }  catch(FileNotFoundException ex) {
            Log.d(TAG, ex.getMessage());
        }  catch(IOException ex) {
            Log.d(TAG, ex.getMessage());
        }
        return  false;
    }
}