import java.io.IOException;

public class ChatBot {
    public static void main(String[] args) throws Exception {

        // Start up bot
        Bot bot = new Bot();

        // Enable debug output
        bot.setVerbose(true);

        // Connect to IRC server
        bot.connect("irc.freenode.net");

        // Join the #pircbot channel
        bot.joinChannel("#pircbot");

        // when yamBot joins the channel, we send an introductory message on how to use the bot
        bot.sendMessage("#pircbot","Hello, I am yamBot, a bot for displaying weather data in the US");
        bot.sendMessage("#pircbot","I can also translate between most ISO 369-1 supported languages");
        bot.sendMessage("#pircbot","Please enter the commands in the following format:");
        bot.sendMessage("#pircbot","!weather <zip>");
        bot.sendMessage("#pircbot", "!translate <conversion> <sentence>");

        // end of program
    }
}
