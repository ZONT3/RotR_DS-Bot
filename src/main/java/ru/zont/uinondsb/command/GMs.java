package ru.zont.uinondsb.command;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.DescribedException;
import ru.zont.dsbot.core.commands.LongCommandAdapter;
import ru.zont.dsbot.core.tools.LOG;
import ru.zont.dsbot.core.tools.Messages;
import ru.zont.dsbot.core.tools.Tools;
import ru.zont.uinondsb.tools.GameMasters;

import java.util.*;

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
                checkArgs(args, 2);
                GameMasters.GM gm = set(GameMasters.getId(args.get(1)), args.size() >= 3 ? args.get(2) : null);
                added(event, gm, args.get(1));
                break;
            case "rm":
                checkArgs(args, 2);
                try {
                    GameMasters.removeGm(args.get(1));
                } catch (GameMasters.NoUpdateException e) {
                    Messages.printError(event.getChannel(), STR.getString("err.general"), STR.getString("comm.gms.err.no_gm"));
                    return;
                }
                ok(event);
                break;
            case "list":
            case "get":
                Messages.sendSplit(
                        event.getChannel(),
                        GameMasters.Msg.gmList(
                                GameMasters.retrieve(),
                                input.hasOpt("s"),
                                input.hasOpt("n"),
                                input.hasOpt("a"),
                                input.hasOpt("o") ),
                        true );
                break;
            default: throw new UserInvalidArgumentException(STR.getString("comm.gms.err.first_arg"));
        }
    }

    private void added(@NotNull MessageReceivedEvent event, GameMasters.GM gm, String s) {
        event.getChannel().sendMessage(new EmbedBuilder()
                .appendDescription("**")
                .appendDescription(String.format(STR.getString("comm.gms.set.ok.title"), s))
                .appendDescription("**\n")
                .appendDescription(String.format(STR.getString("comm.gms.get.steamid"), gm.steamid64))
                .appendDescription("\n")
                .appendDescription(String.format(STR.getString("comm.gms.get.armaname"), gm.armaname))
                .setColor(0x00AA00)
                .build()
        ).queue();
        LOG.d("Assigned GM: %s [%s] by %s", gm.armaname, gm.steamid64, event.getAuthor().getAsTag());
    }

    private void ok(@NotNull MessageReceivedEvent event) {
        event.getChannel().sendMessage(new EmbedBuilder()
                .setColor(0x00AA00)
                .setDescription(":white_check_mark:")
                .build()).queue();
    }

    private GameMasters.GM set(long id, String steamid64) {
        GameMasters.GM gm = new GameMasters.GM();
        gm.steamid64 = steamid64;
        gm.userid = id;
        try {
            GameMasters.setGm(gm);
        } catch (NoSuchElementException e) {
            throw new DescribedException(
                    STR.getString("comm.gms.err.ids.title"),
                    STR.getString("comm.gms.err.ids"));
        }
        return gm;
    }

    private static void checkArgs(ArrayList<String> args, int needed) {
        if (args.size() < needed)
            throw new UserInvalidArgumentException(STR.getString("err.incorrect_args"));
        if (!args.get(1).matches("<@!?\\d+>")
                && !(args.get(0).equalsIgnoreCase("rm") && args.get(1).matches("\\d+")))
            throw new UserInvalidArgumentException(STR.getString("comm.gms.err.second_arg"));
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
        if (!parseInput(this, event)
                .argEquals(Arrays.asList("set", "rm", "add", "del"), 0)) return true;
        if (!Tools.guildAllowed(event.getGuild())) return false;

        final Member member = event.getMember();
        return
                member != null &&
                        (member.hasPermission(Permission.ADMINISTRATOR) ||
                        member.hasPermission(Permission.MANAGE_PERMISSIONS));
    }

    @Override
    public boolean allowForeignGuilds(@NotNull MessageReceivedEvent event) {
        return !parseInput(this, event).argEquals(Arrays.asList("set", "rm", "add", "del"), 0);
    }
}