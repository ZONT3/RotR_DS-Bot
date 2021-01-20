package ru.zont.uinondsb.command;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.commands.Commands;
import ru.zont.dsbot.core.tools.Tools;
import ru.zont.uinondsb.Globals;
import ru.zont.uinondsb.tools.Commons;

import java.sql.*;
import java.util.Properties;

import static ru.zont.dsbot.core.tools.Strings.*;

public class Bind extends CommandAdapter {
    public Bind(ZDSBot bot) throws RegisterException {
        super(bot);
    }

    @Override
    public void onRequest(@NotNull MessageReceivedEvent event) throws UserInvalidArgumentException {
        final Commands.Input input = Commands.parseInput(this, event);
        input.checkArgCount(2);

        long userid = Tools.userMentionToID(input.getArg(0));
        String steamid = Commons.assertSteamID(input.getArg(2));

        upd(userid, steamid);
    }

    private void upd(long userid, String steamid) {
        try (Connection connection = DriverManager.getConnection(Globals.dbConnection);
             Statement st = connection.createStatement()) {
            st.executeUpdate(
                "INSERT INTO profiles (p_guid, p_id_dis) " +
                    "VALUES ('"+steamid+"', '"+userid+"') " +
                    "ON DUPLICATE KEY UPDATE p_id_dis='"+userid+"'");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCommandName() {
        return "bind";
    }

    @Override
    public String getSynopsis() {
        return "bind <@user> <steamid64>";
    }

    @Override
    public String getDescription() {
        return STR.getString("comm.bind.desc");
    }

    @Override
    protected Properties getPropsDefaults() {
        return null;
    }

    @Override
    public boolean checkPermission(MessageReceivedEvent event) {
        if (!Tools.guildAllowed(event.getGuild())) return false;

        final Member member = event.getMember();
        return
                member != null &&
                        (member.hasPermission(Permission.ADMINISTRATOR) ||
                                member.hasPermission(Permission.MANAGE_PERMISSIONS));
    }
}
