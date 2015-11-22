/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.bng.core.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;

import com.google.gson.Gson;

/**
 *
 * @author richa
 */
public class Utility 
{   
	private static Gson gson = new Gson();

	public static int getModValue(String msisdn, int modFactor)
    {
        int mod = Integer.parseInt(msisdn.substring(msisdn.length()-4))%(modFactor);
        return mod;
    }
    
    public static boolean getPercentage(int percentage)
    {
        boolean flag = false;
        try
        {
            Random randomGenerator = new Random();
            int randomNumber = randomGenerator.nextInt(100);
            if(randomNumber <= percentage)
                flag = true;
            Logger.sysLog(LogValues.info, Utility.class.getName(),"Random number = "+randomNumber+", recording = "+flag);
        }
        catch(Exception e)
        {
            Logger.sysLog(LogValues.error, Utility.class.getName(), coreException.GetStack(e));
        }
        return flag;
    }   
    
    public static String callUrl(String httpurl)
    {
        String httpUrlResponse = "";
        try
        {
            //System.out.println("HTTP url = "+httpurl);
            URL url = new URL(httpurl);
            URLConnection uc = url.openConnection();
            HttpURLConnection con = (HttpURLConnection) uc;           
            InputStream rd = con.getInputStream();
            int c = 0;
            while ((c = rd.read()) != -1)
            {
                    httpUrlResponse+= (char) c;
            }
        }
        catch(Exception e)
        {
            Logger.sysLog(LogValues.error, Utility.class.getName(), coreException.GetStack(e));
        }
        return httpUrlResponse.trim();
    }
    
    
    public synchronized static String convertObjectToJsonStr(Object object)
    {    	
    	return gson.toJson(object);
    }
    
    public synchronized static <T> T convertJsonStrToObject(String json, Class<T> classOfT)
    {    	 
    	return gson.fromJson(json,classOfT);
    }
    
    public static byte[] object2byte(Object obj) throws Exception{
            ByteArrayOutputStream byteObject = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteObject);
            objectOutputStream.writeObject(obj);
            objectOutputStream.flush();
            objectOutputStream.close();
            byteObject.close();
            return byteObject.toByteArray();
    }

    public static Object byte2object(byte[] obj) throws Exception{
            ByteArrayInputStream bais = new ByteArrayInputStream(obj); 
            ObjectInputStream object = new ObjectInputStream(bais);
            bais.close();
            object.close();
            return object.readObject();
    }
}
