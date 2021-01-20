package ru.zont.uinondsb.tools;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.Commands;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.dsbot.core.tools.Tools;

import java.util.List;

import static ru.zont.dsbot.core.commands.Commands.parseInput;
import static ru.zont.dsbot.core.tools.Strings.*;

public class Commons {

    public static String trimNick(String nick) {
        return nick.replaceAll("\\[.+]", "")
                .trim();
    }

    public static String countPlayers(int count) {
        return getPlural(count, STR.getString("plurals.players.other"), STR.getString("plurals.players.few"), STR.getString("plurals.players.other"));
    }

    public static String countGMs(int count) {
        return getPlural(count, STR.getString("plurals.gms.one"), STR.getString("plurals.gms.few"), STR.getString("plurals.gms.other"));
    }

    public static boolean isWindows() {
        return (System.getProperty("os.name").contains("win"));
    }

    public static boolean isMac() {
        return (System.getProperty("os.name").contains("mac"));
    }

    public static boolean isUnix() {
        final String os = System.getProperty("os.name");
        return (os.contains("nix")
                || os.contains("nux")
                || os.contains("aix"));
    }

    public static boolean rolesLikePermissions(CommandAdapter adapter, MessageReceivedEvent event, List<String> values) {
        if (!parseInput(adapter, event)
                .argEquals(values, 0)) return true;
        if (!Tools.guildAllowed(event.getGuild())) return false;

        final Member member = event.getMember();
        return
                member != null &&
                        (member.hasPermission(Permission.ADMINISTRATOR) ||
                        member.hasPermission(Permission.MANAGE_PERMISSIONS));
    }

    public static Commands.Router rolesLikeRouter(int index, Commands.Router.Case set, Commands.Router.Case rm, Commands.Router.Case get) {
        return new Commands.Router(index)
                .addCase(set, "set", "add")
                .addCase(rm, "rm", "del")
                .addCase(get, "get", "list");
    }

    public static String getRoleGmID() {
        return Configs.getID("role_gm");
    }

    public static String getChannelStatusID() {
        return Configs.getID("channel_status");
    }

    public static String assertSteamID(String arg) {
        if (!arg.matches("7656\\d+"))
            throw new CommandAdapter.UserInvalidArgumentException(STR.getString("comms.err.invalid_steamid64"));
        return arg;
    }


}
