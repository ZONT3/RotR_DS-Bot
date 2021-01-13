package ru.zont.uinondsb.command.exec;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.Commands;
import ru.zont.dsbot.core.ZDSBot;

import java.util.Properties;

import static ru.zont.dsbot.core.tools.Strings.STR;

public class Cmd extends CommandAdapter {
    public Cmd(ZDSBot bot) throws RegisterException {
        super(bot);
    }

    @Override
    public void onRequest(@NotNull MessageReceivedEvent event) throws UserInvalidArgumentException {
        Commands.call(Exec.class, "--cmd " + Commands.parseRaw(this, event), event, getBot());
    }

    @Override
    public String getCommandName() {
        return "cmd";
    }

    @Override
    public String getSynopsis() {
        return "cmd <command>";
    }

    @Override
    public String getDescription() {
        return STR.getString("comm.cmd.desc");
    }

    @Override
    public boolean checkPermission(MessageReceivedEvent event) {
        return false;
    }

    @Override
    protected Properties getPropsDefaults() {
        return null;
    }
}
