package ru.zont.uinondsb.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.ExternalCallable;
import ru.zont.dsbot.core.commands.NotImplementedException;
import ru.zont.dsbot.core.tools.Tools;
import ru.zont.uinondsb.tools.Commons;

import java.util.*;

import static ru.zont.dsbot.core.commands.Commands.Input;
import static ru.zont.dsbot.core.commands.Commands.parseInput;
import static ru.zont.dsbot.core.tools.Strings.STR;
import static ru.zont.uinondsb.tools.TRoles.*;

public class Roles extends CommandAdapter implements ExternalCallable {
    public Roles(ZDSBot bot) throws RegisterException {
        super(bot);
    }

    @Override
    public void call(Input input) {
        final MessageReceivedEvent event = input.getEvent();
        Commons.rolesLikeRouter(0, this::set, this::rm, this::get)
                .setError(STR.getString("comm.gms.err.first_arg"))
                .addCase((i, e) ->
                        Commons.rolesLikeRouter(1, this::autoSet, this::autoRm, this::autoGet)
                                .acceptInput(i, e), "auto")
                .acceptInput(input, event);
    }

    @Override
    public void onRequest(@NotNull MessageReceivedEvent event) throws UserInvalidArgumentException {
        call(parseInput(this, event));
    }

    private int parseID(String arg) {
        if (!arg.matches("[+-]?\\d+"))
            throw new UserInvalidArgumentException("ID should be an integer number");
        return Integer.parseInt(arg);
    }

    private ArrayList<Profile> fetchProfiles(int id) {
        final ArrayList<Profile> profiles = fetchProfilesWithRoles();
        profiles.removeIf(profile -> !profile.roles.contains(id));
        return profiles;
    }

    private Profile fetchProfile(String argDisID, String argSteamID) {
        long userid = argDisID != null ? Tools.userMentionToID(argDisID) : -1;

        Profile profile;
        String uid;
        if (argSteamID == null) {
            profile = getProfileByDisID(userid);
            uid = profile.uid;
        } else {
            uid = Commons.assertSteamID(argSteamID);
            profile = getProfileByUID(uid);
        }

        if (profile != null && userid < 0) {
            userid = profile.userid;
            if (userid == 0) userid = -1;
        }

        final HashSet<Integer> roles = profile != null ? profile.roles : new HashSet<>();
        return new Profile(uid, userid, roles);
    }

    private void set(Input input, MessageReceivedEvent event) {
        input.checkArgCount(3, true);
        final List<String> args = input.getAllArgs();
        int id = parseID(args.get(1));
        final Profile profile = fetchProfile(args.get(2), args.size() >= 4 ? args.get(3) : null);
        profile.roles.add(id);
        commitRoles(profile.roles, profile.uid, profile.userid, id, "add");
        msgDescribeUpdate(profile, event.getChannel());
    }

    private void rm(Input input, MessageReceivedEvent event) {
        input.checkArgCount(3, true);
        int id = parseID(input.getAllArgs().get(1));

        final String arg = input.getAllArgs().get(2);
        long userid = -1; String steamid = null;
        try { userid = Tools.userMentionToID(arg); }
        catch (UserInvalidArgumentException ignored) { }
        if (userid < 0) {
            try {
                steamid = Commons.assertSteamID(arg);
            } catch (UserInvalidArgumentException ignored) { }
            if (steamid == null)
                throw new UserInvalidArgumentException("Hasn't detected steamid nor discord @mention");
        }

        final Profile profile = fetchProfile(userid < 0 ? null : arg + "", steamid);
        profile.roles.remove(id);
        commitRoles(profile.roles, profile.uid, profile.userid, id, "rm");
        msgDescribeUpdate(profile, event.getChannel());
    }

    private void get(Input input, MessageReceivedEvent event) {
        if (input.getAllArgs().size() < 2) {
            event.getChannel().sendMessage(msgList()).queue();
            return;
        }
        final String arg = input.getAllArgs().get(1);
        long userid = -1; String steamid = null; int id = 0;
        try { userid = Tools.userMentionToID(arg); }
        catch (UserInvalidArgumentException ignored) { }
        if (userid < 0) {
            try {
                steamid = Commons.assertSteamID(arg);
            } catch (UserInvalidArgumentException ignored) { }
            try {
                id = parseID(arg);
            } catch (Throwable e) {
                throw new UserInvalidArgumentException("Hasn't detected steamid nor discord @mention nor role ID");
            }
        }

        if (id != 0) {
            event.getChannel().sendMessage(msgListByID(fetchProfiles(id), id)).queue();
        } else {
            final Profile profile = fetchProfile(userid < 0 ? null : arg, steamid);
            msgDescribeUpdate(profile, event.getChannel(), STR.getString("comm.roles.updated.title.d"));
        }
    }

    private void autoSet(Input input, MessageReceivedEvent event) {
        throw new NotImplementedException();
    }

    private void autoGet(Input input, MessageReceivedEvent event) {
        throw new NotImplementedException();
    }

    private void autoRm(Input input, MessageReceivedEvent event) {
        throw new NotImplementedException();
    }

    @Override
    public String getCommandName() {
        return "roles";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("role");
    }

    @Override
    public String getSynopsis() {
        return "roles add|set|get|list|rm|del|auto ...\n" +
                "roles add|set <ID> <@who> [steamID64]\n" +
                "roles rm|del <@who|steamID64> <ID>\n" +
                "roles get|list [ID|@who]\n" +
                "roles auto add|set|rm|del|list|get ...\n" +
                "roles auto add|set <@ds-role|ds-role-id> <ID>\n" +
                "roles auto rm|del <ID|@ds-role|ds-role-id>\n" +
                "roles auto list|get";
    }

    @Override
    public String getDescription() {
        return STR.getString("comm.roles.desc");
    }

    @Override
    protected Properties getPropsDefaults() {
        return null;
    }

    @Override
    public boolean checkPermission(MessageReceivedEvent event) {
        return Commons.rolesLikePermissions(this, event, Arrays.asList("set", "rm", "add", "del", "auto"));
    }

    @Override
    public boolean allowForeignGuilds(@NotNull MessageReceivedEvent event) {
        return !parseInput(this, event).argEquals(Arrays.asList("set", "rm", "add", "del", "auto"), 0);
    }
}
