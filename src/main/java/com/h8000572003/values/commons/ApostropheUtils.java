package com.h8000572003.values.commons;

import java.util.ArrayList;
import java.util.List;

public class ApostropheUtils {

    private static final List<ICommandString> commands = new ArrayList<>();

    static {
        commands.add(ApostropheUtils::replaceEnd);
        commands.add(ApostropheUtils::replaceStart);
        commands.add(ApostropheUtils::replaceStartWithPercentage);
        commands.add(ApostropheUtils::replaceEndWithPercentage);

    }

    public static String replaceAll(String value) {
        for (ICommandString command : commands) {
            value = command.replace(value);
        }
        return value;
    }

    interface ICommandString {
        String replace(String value);
    }

    /**
     * 'xxx
     *
     * @param value
     * @return
     */

    public static String replaceEnd(String value) {
        return value.replaceAll("\"\\s*'\\s*$", "\" ");
    }

    public static String replaceStart(String value) {
        return value.replaceAll("\\s*'\\s*\"", " \"");
    }

    /**
     * value= '%" + ABC => value=%" + ABC"
     *
     * @param value
     * @return
     */
    public static String replaceStartWithPercentage(String value) {
        return value.replaceAll("\\s*'\\s*%?\"", " %\"");
    }


    /**
     * value=  => value=%" + ABC"
     *
     * @param value
     * @return
     */
    public static String replaceEndWithPercentage(String value) {
        return value.replaceAll("\"\\s*'\\s*%?$", "\"% ");
    }

}
