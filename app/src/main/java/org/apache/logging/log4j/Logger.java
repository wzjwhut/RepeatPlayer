package org.apache.logging.log4j;
import android.util.Log;

public class Logger {
	private final String TAG;
    protected Logger(String tag){
        this.TAG = tag;
    }

    public void info(String format, final Object... arguments){
        if(arguments == null || arguments.length == 0){
            Log.e(TAG, format);
        }else{
            int length = arguments.length;
            if(arguments[length-1] instanceof Throwable){
                final int len = format.length();
                final StringBuilder result = new StringBuilder(len<<1 );
                final int argCount =  arguments.length - 1;
                StringFormatter.formatMessage(result, format, arguments, argCount);
                String msg = result.toString();
                Log.e(TAG, msg, (Throwable)arguments[length-1]);
            }else{
                String msg = StringFormatter.format(format, arguments);
                Log.e(TAG, msg);
            }
        }
    }

    public void error(String format, final Object... arguments){
        if(arguments == null || arguments.length == 0){
            Log.e(TAG, format);
        }else{
            int length = arguments.length;
            if(arguments[length-1] instanceof Throwable){
                final int len = format.length();
                final StringBuilder result = new StringBuilder(len<<1 );
                final int argCount =  arguments.length - 1;
                StringFormatter.formatMessage(result, format, arguments, argCount);
                String msg = result.toString();
                Log.e(TAG, msg, (Throwable)arguments[length-1]);
            }else{
                String msg = StringFormatter.format(format, arguments);
                Log.e(TAG, msg);
            }
        }
    }
}