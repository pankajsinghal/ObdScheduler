package com.bng.core.utils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Title: coreException
 * Description:  This class do Logs and other error notifications in his constructor
 * Copyright:    Copyright (c) 2013
 * Company: blackngreen
 * Last Modify: 2001/11/26 (yyyy/mm/dd)
 *
 * @author agrim
 * @version 1.0
 *
 */



public class coreException extends Throwable
{
    /**
     * This static method return the call stack for a Exception in a String
     */
    static public String GetStack( Throwable E )
    {
        String szResult = "There was some error creating the Stack Frame";
        try
        {
            ByteArrayOutputStream Stack = new ByteArrayOutputStream();
            PrintStream PrintStack = null;
            if( Stack != null )
            {
                PrintStack = new PrintStream( Stack );
                if( PrintStack != null )
                {
                    E.printStackTrace( PrintStack );
                    szResult = Stack.toString();
                }
            }
        }
        catch( Exception e )
        {
            szResult += ( "\r\n\t"+e.toString() );
        }
        return "\r\n\t"+szResult;
    }



    /**
     * This Constructor Only does a Syslog.
     */

    public coreException( int severity, String applicationName, String message )
    {
        this( severity, applicationName, message, false );
    }

    public coreException( int severity, String applicationName, String message, boolean bPrintStack )
    {
        super( message );
        Logger.sysLog( severity, applicationName, message+(bPrintStack?GetStack( this ):"" ) );
    }


    /**
    *This Constructor Only does a Syslog.
    *If Applicattionname is Null or Empty uses Module.getClass().getName() to get the module name
    */
    public coreException( int severity, String applicationName, String message, coreException Module )
    {
        this( severity, applicationName, message, Module, false );
    }

    /**
    * Does the same that the previous Constructor, but you can decided if you want also or not
    * a Snapshot of the call stack with the parmeter bPrintStack
    */
    public coreException( int severity, String applicationName, String message, coreException Module, boolean bPrintStack )
    {
        super( message );
        if( ( applicationName != null ) && ( applicationName.length() != 0 ) )
            Logger.sysLog( severity, applicationName, message+(bPrintStack?GetStack( this ):"") );
        else
        	Logger.sysLog( severity, Module.getClass().getName(), message+(bPrintStack?GetStack( this ):"") );
    }


    /**
     * This constructor does a Syslog and at the same time, write in the Context Server Variable, stored in "Paramerter", the Value
     * in "Value", if the destination web page has a parameter $"Parameter"$ the "Value" is inserted in the web page.
     * So we can say to the user that an error has happened.
     * If Applicattionname is Null or Empty uses coreException.getClass().getName() to get the class name
     */
    public coreException( int severity, String applicationName, String message, coreException Module, String Parameter, String Value )
    {
        this( severity, applicationName, message, Module, Parameter, Value, false );
    }


    /**
    * Does the same that the previous Constructor, but you can decided if you want also or not
    * a Snapshot of the call stack with the parmeter bPrintStack
    */
    public coreException( int severity, String applicationName, String message, coreException Module, String Parameter, String Value, boolean bPrintStack )
    {
        super( message );
        if( ( applicationName != null ) && ( applicationName.length() != 0 ) )
        	Logger.sysLog( severity, applicationName, message+(bPrintStack?GetStack( this ):"") );
        else
        	Logger.sysLog( severity, Module.getClass().getName(), message+(bPrintStack?GetStack( this ):"") );
       // Module.writeContext( Parameter, Value );
    }
}
