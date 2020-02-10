import java.util.*;
import java.io.*;
import java.math.*;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Solution {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int N = in.nextInt(); // Number of elements which make up the association table.
        in.nextLine();
        int Q = in.nextInt(); // Number Q of file names to be analyzed.
        in.nextLine();
        HashMap<String,String> mimeTypes = new HashMap<String,String>();
        for (int i = 0; i < N; i++) {
            String EXT = in.next(); // file extension
            String MT = in.next(); // MIME type.
            mimeTypes.put(EXT.toUpperCase(),MT);
            in.nextLine();
        }
        /*
        Set<String> keys = mimeTypes.keySet();
        for (String key : keys) {
            System.out.println(key + " " + mimeTypes.get(key));
        }
        */
        String FNAME = "";
        String EXT = "";
        int dotIndex = -1;
        for (int i = 0; i < Q; i++) {
            FNAME = in.nextLine(); // One file name per line.
            if (FNAME.contains(".")) {
                dotIndex = FNAME.lastIndexOf(".");
                EXT = FNAME.substring(dotIndex + 1);
                if (mimeTypes.containsKey(EXT.toUpperCase()))
                    System.out.println(mimeTypes.get(EXT.toUpperCase()));
                else
                    System.out.println("UNKNOWN");
            }
            else
                System.out.println("UNKNOWN");
        }
    }
}