import dev.arbjerg.lavalink.client.LavalinkClient
import dev.arbjerg.lavalink.client.NodeOptions
import dev.arbjerg.lavalink.client.player.PlaylistLoaded
import dev.arbjerg.lavalink.client.player.SearchResult
import dev.arbjerg.lavalink.client.player.TrackLoaded
import dev.arbjerg.lavalink.libraries.jda.JDAVoiceUpdateListener
import io.github.cdimascio.dotenv.Dotenv
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
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
            .setServerUri("ws://localhost:2333")
            .setPassword("youshallnotpass")
            .build()
    )

    val jda =
        JDABuilder.createDefault(discordToken)
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
            .setVoiceDispatchInterceptor(
                JDAVoiceUpdateListener(lavalinkClient)
            )
            .addEventListeners(BotCommands())
            .build()

    jda.awaitReady()

    jda.guilds.forEach { guild ->
        guild.updateCommands()
            .addCommands(
                Commands.slash("ping", "Check if bot is alive"),

                Commands.slash(
                    "join",
                    "Join your voice channel"
                ),

                Commands.slash(
                    "leave",
                    "Leave voice channel"
                ),

                Commands.slash(
                    "play",
                    "Play from YouTube"
                )
                    .addOption(
                        OptionType.STRING,
                        "query",
                        "YouTube link or search",
                        true
                    )
            )
            .queue()
    }

    println("Bot started with Lavalink")
}

class BotCommands : ListenerAdapter() {

    override fun onSlashCommandInteraction(
        event: SlashCommandInteractionEvent
    ) {
        when (event.name) {
            "ping" -> event.reply("Pong!").queue()
            "join" -> join(event)
            "leave" -> leave(event)
            "play" -> play(event)
        }
    }

    private fun join(
        event: SlashCommandInteractionEvent
    ) {

        val channel =
            event.member
                ?.voiceState
                ?.channel

        if (channel == null) {
            event.reply(
                "Join voice channel first."
            )
                .setEphemeral(true)
                .queue()

            return
        }

        event.jda
            .directAudioController
            .connect(channel)

        event.reply(
            "Joined ${channel.name}"
        )
            .queue()
    }

    private fun leave(
        event: SlashCommandInteractionEvent
    ) {

        val guild =
            event.guild ?: return

        event.jda
            .directAudioController
            .disconnect(guild)

        event.reply(
            "Disconnected."
        )
            .queue()
    }

    private fun play(
        event: SlashCommandInteractionEvent
    ) {

        event.deferReply().queue()

        val guild =
            event.guild ?: return

        val channel =
            event.member
                ?.voiceState
                ?.channel

        if (channel == null) {

            event.hook
                .sendMessage(
                    "Join voice channel first."
                )
                .queue()

            return
        }

        if (
            !guild.selfMember
                .voiceState!!
                .inAudioChannel()
        ) {
            event.jda
                .directAudioController
                .connect(channel)
        }

        val raw =
            event.getOption(
                "query"
            )!!
                .asString

        val query =
            if (raw.startsWith("http"))
                raw
            else
                "ytsearch:$raw"

        val link =
            lavalinkClient
                .getOrCreateLink(
                    guild.idLong
                )

        link.loadItem(query)
            .subscribe { result ->

                val track =
                    when (result) {

                        is TrackLoaded ->
                            result.track

                        is SearchResult ->
                            result.tracks.firstOrNull()

                        is PlaylistLoaded ->
                            result.tracks.firstOrNull()

                        else ->
                            null
                    }

                if (track == null) {

                    event.hook
                        .sendMessage(
                            "No track found."
                        )
                        .queue()

                    return@subscribe
                }

                val player =
                    link.createOrUpdatePlayer()

                player.setVolume(100)

                player.setPaused(false)

                player
                    .setTrack(track)
                    .subscribe {

                        event.hook
                            .sendMessage(
                                "Now playing: ${track.info.title}"
                            )
                            .queue()
                    }
            }
    }
}