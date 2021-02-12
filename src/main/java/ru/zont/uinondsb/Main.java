package ru.zont.uinondsb;

import ru.zont.dsbot.core.ZDSBot;
import ru.zont.dsbot.core.commands.CommandAdapter;
import ru.zont.dsbot.core.handler.LStatusHandler;
import ru.zont.dsbot.core.tools.Configs;
import ru.zont.uinondsb.command.exec.Cmd;
import ru.zont.uinondsb.command.exec.Do;
import ru.zont.uinondsb.command.exec.Exec;
import ru.zont.uinondsb.command.exec.Term;
import ru.zont.uinondsb.command.*;
import ru.zont.uinondsb.listeners.HCharacters;
import ru.zont.uinondsb.listeners.LServerStatus;

import javax.security.auth.login.LoginException;
import java.util.Properties;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        Configs.setGlobalPropsDefaults(new Properties(){{
            setProperty("command_prefix", "t.");
            setProperty("TA_IDS", "331524458806247426,183600856560566272");
            setProperty("ALLOWED_SERVERS", "795084260083236874,331526118635208716");
            setProperty("channel_status", "799289220275503114");
            setProperty("channel_charlog", "807823130486898698");
            setProperty("role_gm", "795084260208934926");
        }});
        Configs.writeDefaultGlobalProps();

        handleArguments(args);

        ZDSBot bot = new ZDSBot(args[0], Globals.version,
                "", "");
        registerAdapters(bot);
        bot.create().awaitReady();
    }

    private static void registerAdapters(ZDSBot bot) {
        // Нет, я не дегенерад, просто Reflections перестали работать на линуксе какого-то хуя. Помогите мне.
        bot.commandAdapters = new CommandAdapter[]{
                new Roles(bot),
                new Cmd(bot),
                new Do(bot),
                new Exec(bot),
                new Term(bot),
                new Clear(bot),
                new Config(bot),
                new GMs(bot),
                new Help(bot),
                new Ping(bot),
                new Say(bot)
        };
        bot.statusHandlers = new LStatusHandler[0];
        bot.getJdaBuilder().addEventListeners((Object[]) bot.statusHandlers);
        bot.getJdaBuilder().addEventListeners(new LServerStatus(), new HCharacters(bot));
    }

    private static void handleArguments(String[] args) throws LoginException, IllegalArgumentException {
        if (args.length < 2) throw new LoginException("Too few arguments");

        Globals.dbConnection = args[1];

        if (args.length >= 3 && args[2].matches("\\d+"))
            Globals.gamePort = Integer.parseInt(args[2]);
    }
}
