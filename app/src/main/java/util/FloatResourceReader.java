package util;

import android.content.Context;
import android.content.res.Resources;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class FloatResourceReader {
    public static float[] readFloatFileFromResource( Context context, int resourceId ) {
        ArrayList<Float> list = new ArrayList<Float>();
        Scanner scan = null;

        try {
            InputStream inputStream = context.getResources().openRawResource( resourceId );
            InputStreamReader inputStreamReader = new InputStreamReader( inputStream );

            scan = new Scanner( inputStreamReader );
            scan.useLocale(Locale.US);
            scan.useDelimiter( ",| |\t|\n" );

        } catch( Exception e ) {
            e.getMessage();
        }

        while( scan.hasNext()) {
            try {
                if( scan.hasNextFloat( )) {
                    float f = scan.nextFloat( );
                    //System.out.print( "<" + f + "> " );
                    list.add( f );
                } else {
                    String str = scan.next( );
                    if( str.contains( "\n" )) {
                        scan.nextLine( );
                    }
                    if( str.contains( "//" )) {
                        str = scan.nextLine( );
                    }
                }
            } catch ( Exception e ) {
                e.getMessage();
            }
        }
        float rtnArray[] = new float[ list.size( )];
        for( int i = 0; i < list.size( ); i++ ) {
            rtnArray[ i ] = list.get( i );
        }
        return rtnArray;
    }
}
