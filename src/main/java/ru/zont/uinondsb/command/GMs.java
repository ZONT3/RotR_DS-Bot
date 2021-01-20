package ru.zont.uinondsb.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.Commands;
import ru.zont.dsbot.core.commands.LongCommandAdapter;
import ru.zont.dsbot.core.tools.Messages;
import ru.zont.uinondsb.tools.Commons;
import ru.zont.uinondsb.tools.TGameMasters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import static ru.zont.dsbot.core.commands.Commands.Input;
import static ru.zont.dsbot.core.commands.Commands.parseInput;
import static ru.zont.dsbot.core.tools.Strings.STR;


public class GMs extends LongCommandAdapter {
    public GMs(ZDSBot bot) throws RegisterException {
        super(bot);
    }

    @Override
    public void onRequestLong(@NotNull MessageReceivedEvent event) throws UserInvalidArgumentException {
        Input input = parseInput(this, event);
        ArrayList<String> args = input.getArgs();
        if (args.size() < 1)
            throw new UserInvalidArgumentException(STR.getString("err.incorrect_args"));
        switch (args.get(0).toLowerCase()) {
            case "set":
            case "add":
                Commands.call(Roles.class,
                        "add 1 " + input.getArg(1) + (input.getArg(2) != null
                                ? " " + input.getArg(2) : ""),
                        event,
                        getBot());
                break;
            case "rm":
            case "del":
                Commands.call(Roles.class, "rm 1 " + input.getArg(1), event, getBot());
                break;
            case "list":
            case "get":
                Messages.sendSplit(
                        event.getChannel(),
                        TGameMasters.Msg.gmList(
                                TGameMasters.retrieve(),
                                input.hasOpt("s"),
                                input.hasOpt("n"),
                                input.hasOpt("a"),
                                input.hasOpt("o") ),
                        true );
                break;
            default: throw new UserInvalidArgumentException(STR.getString("comm.gms.err.first_arg"));
        }
    }

    @Override
    public String getCommandName() {
        return "gm";
    }

    @Override
    protected Properties getPropsDefaults() {
        return null;
    }

    @Override
    public String getSynopsis() {
        return  "gm [-snao] set|add|get|list|rm|del ...\n" +
                "gm set|add <@user> [steamid64]\n" +
                "gm [-snao] get|list\n" +
                "gm rm|del <@user>|<steamid64>";
    }

    @Override
    public String getDescription() {
        return STR.getString("comm.gms.desc");
    }

    @Override
    public boolean checkPermission(MessageReceivedEvent event) {
        return Commons.rolesLikePermissions(this, event, Arrays.asList("set", "rm", "add", "del"));
    }

    @Override
    public boolean allowForeignGuilds(@NotNull MessageReceivedEvent event) {
        return !parseInput(this, event).argEquals(Arrays.asList("set", "rm", "add", "del"), 0);
    }
}
