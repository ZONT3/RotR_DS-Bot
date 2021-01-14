package ru.zont.uinondsb.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.NotImplementedException;
import ru.zont.uinondsb.tools.Commons;
import ru.zont.uinondsb.tools.TRoles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static ru.zont.dsbot.core.commands.Commands.Input;
import static ru.zont.dsbot.core.commands.Commands.parseInput;
import static ru.zont.dsbot.core.tools.Strings.STR;

public class Roles extends CommandAdapter {
    public Roles(ZDSBot bot) throws RegisterException {
        super(bot);
    }

    @Override
    public void onRequest(@NotNull MessageReceivedEvent event) throws UserInvalidArgumentException {
        Input input = parseInput(this, event);
        Commons.rolesLikeRouter(0, this::set, this::rm, this::get)
                .setError(STR.getString("comm.gms.err.first_arg"))
                .addCase((i, e) ->
                        Commons.rolesLikeRouter(1, this::autoSet, this::autoRm, this::autoGet)
                        .acceptInput(i, e), "auto")
                .acceptInput(input, event);
    }

    private void set(Input input, MessageReceivedEvent event) {
        throw new NotImplementedException();
    }

    private void rm(Input input, MessageReceivedEvent event) {
        throw new NotImplementedException();
    }

    private void get(Input input, MessageReceivedEvent event) {
        if (input.getArg(1) == null) {
            event.getChannel().sendMessage(TRoles.msgList()).queue();
            return;
        }
        throw new NotImplementedException();
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
                "roles rm|del <ID|@who|steamID64>\n" +
                "roles get|list [ID]\n" +
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
