package ru.zont.uinondsb.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.Commands;
import ru.zont.dsbot.core.commands.DescribedException;
import ru.zont.dsbot.core.tools.Messages;

import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ru.zont.dsbot.core.tools.Strings.STR;

public class Clear extends CommandAdapter {
    public Clear(ZDSBot bot) throws RegisterException {
        super(bot);
    }

    @Override
    public void onRequest(@NotNull MessageReceivedEvent event) throws UserInvalidArgumentException {
        final Commands.Input input = Commands.parseInput(this, event);
        if (input.getArgs().size() < 1)
            throw new UserInvalidArgumentException(STR.getString("err.insufficient_args"));

        TextChannel toClear;
        int amount;
        if (input.getArgs().size() == 1) {
            if (!input.getArg(0).matches("\\d+"))
                throw new UserInvalidArgumentException(STR.getString("comm.clear.err.syntax"));
            toClear = event.getTextChannel();
            amount = Integer.parseInt(input.getArg(0));
        } else {
            final String ref = input.getArg(0);
            final Matcher matcher = Pattern.compile("<#!?(\\d+)>").matcher(ref);
            if (!matcher.find() || !input.getArg(1).matches("\\d+"))
                throw new UserInvalidArgumentException(STR.getString("comm.clear.err.syntax"));
            if (!event.isFromGuild())
                throw new UserInvalidArgumentException(STR.getString("comm.clear.err.pm"), false);
            toClear = event.getGuild().getTextChannelById(matcher.group(1));
            amount = Integer.parseInt(input.getArg(1));
        }
        if (toClear == null)
            throw new DescribedException(STR.getString("err.general"), STR.getString("comm.clear.err.null_channel"));

        final boolean b = toClear == event.getTextChannel();
        if (b) event.getMessage().delete().queue();
        else Messages.addOK(event.getMessage());
        for (Message message: toClear.getHistory().retrievePast(b ? amount : amount + 1).complete())
            message.delete().queue();
    }

    @Override
    public String getCommandName() {
        return "clear";
    }

    @Override
    public String getSynopsis() {
        return "clear [#channel] <amount>";
    }

    @Override
    public String getDescription() {
        return STR.getString("comm.clear.desc");
    }

    @Override
    protected Properties getPropsDefaults() {
        return null;
    }

    @Override
    public boolean checkPermission(MessageReceivedEvent event) {
        Member member = event.getMember();
        if (member == null) return false;
        return member.hasPermission(Permission.MESSAGE_MANAGE);
    }
}
