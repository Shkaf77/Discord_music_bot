import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.NodeOptions
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import io.github.cdimascio.dotenv.Dotenv
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.requests.GatewayIntent

lateinit var lavalinkClient: LavalinkClient

fun main() {
    val env = Dotenv.load()

    val discordToken =
        env["DISCORD_TOKEN"]
            ?: error("DISCORD_TOKEN is missing")

    val discordBotId =
        env["DISCORD_BOT_ID"]?.toLongOrNull()
            ?: error("DISCORD_BOT_ID is missing")

    lavalinkClient = LavalinkClient(discordBotId)

    lavalinkClient.addNode(
        NodeOptions.Builder()
            .setName("local")
            .setServerUri("http://localhost:2333")
            .setPassword("youshallnotpass")
            .build()
    )

    val jda = JDABuilder.createDefault(discordToken)
        .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
        .setVoiceDispatchInterceptor(JDAVoiceUpdateListener(lavalinkClient))
        .addEventListeners(BotCommands())
        .build()

    jda.awaitReady()

    jda.updateCommands().addCommands(
        Commands.slash("ping", "Check if bot is alive"),
        Commands.slash("join", "Join your voice channel through Lavalink"),
        Commands.slash("leave", "Leave voice channel")
    ).queue()

    println("Bot started with Lavalink")
}

class BotCommands : ListenerAdapter() {
    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        when (event.name) {
            "ping" -> {
                event.reply("Pong!").queue()
            }

            "join" -> {
                val guild = event.guild ?: return
                val voiceChannel = event.member?.voiceState?.channel

                if (voiceChannel == null) {
                    event.reply("First of all, join the voice channel")
                        .setEphemeral(true)
                        .queue()
                    return
                }

                guild.jda.directAudioController.connect(voiceChannel)

                event.reply("Joined voice channel: ${voiceChannel.name}")
                    .queue()
            }

            "leave" -> {
                val guild = event.guild ?: return

                guild.jda.directAudioController.disconnect(guild)

                event.reply("Disconnected voice channel.")
                    .queue()
            }
        }
    }
}