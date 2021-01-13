package ru.zont.uinondsb.command;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;

import java.util.Properties;

import static ru.zont.dsbot.core.tools.Strings.*;

public class Say extends CommandAdapter {
    public Say(ZDSBot bot) throws RegisterException {
        super(bot);
    }

    @Override
    public void onRequest(@NotNull MessageReceivedEvent event) throws UserInvalidArgumentException {
        if (!event.isFromType(ChannelType.PRIVATE)) return;
        String content = event.getMessage().getContentRaw();
        String[] s = content.split(" ");
        if (s.length < 3) throw new UserInvalidArgumentException(STR.getString("err.insufficient_args"));
        if (!s[1].matches("\\d+")) throw new UserInvalidArgumentException("First arg should be a channel ID!");
        String id = s[1];

        TextChannel channel = null;
        for (Guild guild: event.getJDA().getGuilds()) {
            channel = guild.getTextChannelById(id);
            if (channel != null) break;
        }
        if (channel == null) throw new UserInvalidArgumentException("Cannot find channel");

        channel.sendMessage(content.replaceFirst("[^ ]+ \\d+ ", "")).queue();
    }

    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public String getCommandName() {
        return "say";
    }

    @Override
    protected Properties getPropsDefaults() {
        return null;
    }

    @Override
    public String getSynopsis() {
        return "say <channelID> <any text>";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public boolean checkPermission(MessageReceivedEvent event) {
        return false;
    }
}
