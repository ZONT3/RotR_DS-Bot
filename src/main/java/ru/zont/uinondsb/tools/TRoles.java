package ru.zont.uinondsb.tools;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import ru.zont.dsbot.core.tools.Strings;

import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.zont.dsbot.core.tools.Strings.*;

public class TRoles {
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

    public static MessageEmbed msgList() {
        return new EmbedBuilder()
                .setTitle(STR.getString("comm.roles.get.title"))
                .setDescription(STR.getString("comm.roles.get"))
                .setColor(0xd700e7)
                .build();
    }
}
