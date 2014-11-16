package uk.org.ngo.squeezer;


import android.app.Application;
import android.content.Context;
// Trick to make the app context useful available everywhere.
// See http://stackoverflow.com/questions/987072/using-application-context-everywhere

public class Squeezer extends Application {

//    private static Context instance;
    private static Context instance = null;


    public static Context getInstance() {
        return Squeezer.instance;
    }

    public static Context getContext(){
        return Squeezer.instance;
        // or return instance.getApplicationContext();
    }

    @Override
    public void onCreate() {
        Squeezer.instance = this;
        super.onCreate();
    }

    private Squeezer()
    {
        Squeezer.instance = this;
    }

    public static void setContext(Context Context){
        Squeezer.instance = Context;
    }
}

