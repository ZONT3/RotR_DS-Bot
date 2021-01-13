package ru.zont.uinondsb.tools;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Roles {
    public static final int ZEUS = 1;

    public static HashSet<Integer> fromString(String list) {
        final Matcher matcher = Pattern.compile("\\d+").matcher(list);
        HashSet<Integer> res = new HashSet<>();
        while (matcher.find())
            res.add(Integer.parseInt(matcher.group()));
        return res;
    }

    public static String fromSet(HashSet<Integer> set) {
        StringBuilder sb = new StringBuilder("[ ");
        boolean first = true;
        for (Integer integer: set) {
            if (!first)
                sb.append(", ");
            else first = false;
            sb.append(integer);
        }
        return sb.append(" ]").toString();
    }
}
