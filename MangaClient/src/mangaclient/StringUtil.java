/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mangaclient;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author David Huynh
 */
public final class StringUtil {

    //Doesn't Instantiate
    private StringUtil() {
    }

    ;
	
    /*
     * Checks if input contains a number
     */
    public static boolean containsNum(String input) {
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Checks to see if parsing the number is possible
     */
    public static boolean isNum(String input) {
        for (char c : input.toCharArray()) {
            if (!Character.isDigit(c) || c != '.') {
                //Special case for decimals
                return false;
            }
        }
        return true;
    }

    public static boolean isValidCharacter(char input) {
        char[] validChar = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-._~:/?#[]@!$&'()*+,;=;"
                .toCharArray();

        for (char c : validChar) {
            if (c == input) {
                return true;
            }
        }
        return false;
    }

    /*
     * Removes trailing white spaces from a String
     */
    public static String removeTrailingWhiteSpaces(String input) {
        while (input.length() >= 1 && input.charAt(input.length() - 1) == ' ') {
            //Removes trailing whitespaces
            input = input.substring(0, input.length() - 1);
        }
        return input;
    }

    public static String[] formatChapterNames(String[] input) {
        for (int i = 0; i < input.length; i++) {
            input[i] = formatChapterNames(input[i]);
        }
        return input;
    }

    public static String formatChapterNames(String input) {
        input = removeTrailingWhiteSpaces(input);
        input = input.substring(input.lastIndexOf(' ') + 1);
        return "Ch: " + input;
    }

    public static String titleCase(String realName) {
        String space = " ";
        String[] names = realName.split(space);
        StringBuilder b = new StringBuilder();
        for (String name : names) {
            if (name == null || name.isEmpty()) {
                b.append(space);
                continue;
            }

            b.append(name.substring(0, 1).toUpperCase())
                    .append(name.substring(1).toLowerCase())
                    .append(space);
        }
        return b.toString();
    }

    /**
     * Determines the index of the String in the an array of Strings
     *
     * @param input The array you want to search through
     * @param searchTerm The String you want to search
     * @return The index of the searchTerm in the input or -1 if it is not
     * found.
     */
    public static int indexOf(String[] input, String searchTerm) {
        for (int i = 0; i < input.length; i++) {
            if (searchTerm.equals(input[i])) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Downloads the text from a specified URL
     *
     * @param url The URL you want to get the text from.
     * @return The downloaded text
     * @throws IOException If something went wrong
     */
    public static String urlToText(String url) throws IOException {
        return urlToText(new URL(url));
    }

    /**
     * Downloads the text from the specified URL
     *
     * @param url The URL you want to get the text from
     * @return The downloaded text.
     * @throws IOException If something went wrong
     */
    public static String urlToText(URL url) throws IOException {
        URLConnection urlConn = url.openConnection(); //Open connection
        Reader r = new java.io.InputStreamReader(urlConn.getInputStream(), Charset.forName("UTF-8"));//Gets Data Converts to string
        StringBuilder buf = new StringBuilder();

        while (true) {
            //Reads String from buffer
            int ch = r.read();
            if (ch < 0) {
                break;
            }
            buf.append((char) ch);
        }
        String str = buf.toString();
        r.close();
        return str;
    }

    /**
     * Converts a collection of Strings into one long String.
     *
     * @param collection The collection you want to get the String from
     * @return The String generated from the collection
     */
    public static String concatCollection(Collection<String> collection) {
        StringBuilder sb = new StringBuilder(collection.size());
        for (String s : collection) {
            sb.append(String.valueOf(s));
        }
        return sb.toString();
    }

    /**
     * Removes null entries from an array of Strings
     *
     * @param strs The String you want to remove the null entries from
     * @return The cleaned array
     */
    public static String[] cleanArray(String[] strs) {
        List<String> list = new ArrayList<String>();
        for (String s : strs) {
            if (s != null && s.length() > 0) {
                list.add(s);
            }
        }
        return list.toArray(new String[list.size()]);
    }

    /**
     * Strips the quotes and gets the text between them
     *
     * @param input The String
     * @return The stripped String
     */
    public static String stripQuotes(String input) {
        return input.substring(input.indexOf('"') + 1, input.lastIndexOf('"'));
    }
}
