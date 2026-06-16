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
import dev.arbjerg.lavalink.client.event.TrackEndEvent
import dev.arbjerg.lavalink.protocol.v4.Playlist

lateinit var lavalinkClient: LavalinkClient
val musicQueues = mutableMapOf<Long, MusicQueue>()
val activePlayers = mutableSetOf<Long>()
val currentTracks = mutableMapOf<Long, String>()
val playlistBoxes = mutableMapOf<Long, PlaylistBox>()

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

    lavalinkClient.on(TrackEndEvent::class.java).subscribe { event ->
        val box = playlistBoxes[event.guildId]
        val boxTrack = box?.next()

        val nextTrack =
            if (boxTrack != null) {
                boxTrack
            } else {
                val queue = musicQueues[event.guildId]
                queue?.next()
            }

        if (nextTrack == null) {
            activePlayers.remove(event.guildId)
            currentTracks.remove(event.guildId)
            return@subscribe
        }

        val link = lavalinkClient.getOrCreateLink(event.guildId)
        val player = link.createOrUpdatePlayer()

        player.setVolume(100)
        player.setPaused(false)

        player.setTrack(nextTrack).subscribe {
            currentTracks[event.guildId] = nextTrack.info.title
        }
    }

    val jda =
        JDABuilder.createDefault(discordToken)
            .enableIntents(GatewayIntent.GUILD_VOICE_STATES)
            .setVoiceDispatchInterceptor(
                JDAVoiceUpdateListener(lavalinkClient)
            ).addEventListeners(BotCommands()).build()

    jda.awaitReady()

    jda.guilds.forEach { guild ->
        guild.updateCommands().addCommands(
            Commands.slash(
                "ping",
                "Check if bot is alive"
            ),

            Commands.slash(
                "queue",
                "Show current queue"
            ),

            Commands.slash(
                "skip",
                "Skip current track"
            ),

            Commands.slash(
                "pause",
                "Pause current track"
            ),

            Commands.slash(
                "resume",
                "Resume current track"
            ),

            Commands.slash("volume", "Set player volume")
                .addOption(
                    OptionType.INTEGER,
                    "level",
                    "Volume from 1 to 100",
                    true
                ),

            Commands.slash("nowplaying", "Show current track"),

            Commands.slash("createplaylist", "Create playlist inside the box")
                .addOption(
                    OptionType.STRING,
                    "name",
                    "Playlist name",
                    true
                ),

            Commands.slash("addboxtrack", "Add track to playlist inside the box")
                .addOption(
                    OptionType.STRING,
                    "playlist",
                    "Playlist name",
                    true
                )
                .addOption(
                    OptionType.STRING,
                    "link",
                    "YouTube or YouTube Music track link",
                    true
                ),

            Commands.slash("startbox", "Start round-robin box playback"),

            Commands.slash("boxstatus", "Show playlist box status"),

            Commands.slash("stopbox", "Stop and clear playlist box"),

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
            ).addOption(
                OptionType.STRING,
                "query",
                "YouTube link or search",
                true
            )
        ).queue()
    }

    println("Bot started")
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
            "queue" -> showQueue(event)
            "skip" -> skip(event)
            "pause" -> pause(event)
            "resume" -> resume(event)
            "volume" -> volume(event)
            "nowplaying" -> nowPlaying(event)
            "createplaylist" -> createPlaylist(event)
            "addboxtrack" -> addBoxTrack(event)
            "startbox" -> startBox(event)
            "boxstatus" -> boxStatus(event)
            "stopbox" -> stopBox(event)
        }
    }

    private fun join(
        event: SlashCommandInteractionEvent
    ) {

        val channel = event.member?.voiceState?.channel

        if (channel == null) {
            event.reply(
                "Join voice channel first."
            ).setEphemeral(true).queue()

            return
        }

        event.jda.directAudioController.connect(channel)

        event.reply(
            "Joined ${channel.name}"
        ).queue()
    }

    private fun leave(
        event: SlashCommandInteractionEvent
    ) {

        val guild = event.guild ?: return

        event.jda.directAudioController.disconnect(guild)

        event.reply(
            "Disconnected."
        ).queue()
    }

    private fun play(
        event: SlashCommandInteractionEvent
    ) {

        event.deferReply().queue()

        val guild = event.guild ?: return
        val channel = event.member?.voiceState?.channel

        if (channel == null) {
            event.hook.sendMessage(
                "Join voice channel first."
            ).queue()

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
            )!!.asString

        val query =
            if (raw.startsWith("http")) {
                raw
            } else {
                "ytsearch:$raw"
            }

        val link = lavalinkClient.getOrCreateLink(
            guild.idLong
        )

        link.loadItem(query).subscribe {
                result ->

            val track =
                when (result) {

                    is TrackLoaded ->
                        result.track

                    is SearchResult ->
                        result.tracks.firstOrNull()

                    is PlaylistLoaded ->
                        result.tracks.firstOrNull()

                    else -> null
                }

            if (track == null) {
                event.hook.sendMessage("No track found.").queue()
                return@subscribe
            }

            val playableTrack = track

            val queue = musicQueues.getOrPut(guild.idLong) {
                MusicQueue()
            }

            val player = link.createOrUpdatePlayer()

            player.setVolume(100)

            if (!activePlayers.contains(guild.idLong)) {
                player.setPaused(false)

                player.setTrack(playableTrack)
                    .subscribe {
                        currentTracks[guild.idLong] = playableTrack.info.title

                        activePlayers.add(guild.idLong)

                        event.hook.sendMessage(
                            "Now playing: ${playableTrack.info.title}"
                        ).queue()
                    }
            } else {
                queue.add(playableTrack)

                event.hook.sendMessage(
                    "Added to queue: ${playableTrack.info.title}"
                ).queue()
            }
        }
    }

    private fun showQueue(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val queue = musicQueues[guild.idLong]

        if (queue == null) {
            event.reply("Queue is empty.").queue()
            return
        }

        val message =
            queue.list()
                .take(10)
                .mapIndexed { index, track ->
                    "${index + 1}. ${track.info.title}"
                }
                .joinToString("\n")

        event.reply("Current queue: \n$message").queue()
    }

    private fun skip(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val queue = musicQueues[guild.idLong]
        val nextTrack = queue?.next()
        val link = lavalinkClient.getOrCreateLink(guild.idLong)
        val player = link.createOrUpdatePlayer()

        if (nextTrack == null) {
            activePlayers.remove(guild.idLong)
            currentTracks.remove(guild.idLong)
            player.setTrack(null).subscribe {
                event.reply("Skipped. Queue is empty").queue()
            }
            return
        }

        val trackToPlay = nextTrack

        player.setTrack(trackToPlay).subscribe {
            currentTracks[guild.idLong] = trackToPlay.info.title

            event.reply("Skipped. Now playing: ${trackToPlay.info.title}").queue()
        }
    }

    private fun pause(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val player = lavalinkClient
            .getOrCreateLink(guild.idLong)
            .createOrUpdatePlayer()

        player.setPaused(true).subscribe{
            event.reply("Paused.").queue()
        }
    }

    private fun resume(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val player = lavalinkClient
            .getOrCreateLink(guild.idLong)
            .createOrUpdatePlayer()

        player.setPaused(false).subscribe {
            event.reply("Resumed.").queue()
        }
    }

    private fun volume(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val level = event.getOption("level")!!.asInt.coerceIn(1, 100)
        val player = lavalinkClient
            .getOrCreateLink(guild.idLong)
            .createOrUpdatePlayer()

        player.setVolume(level).subscribe {
            event.reply("Volume set to $level%.").queue()
        }
    }

    private fun nowPlaying(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val title = currentTracks[guild.idLong]

        if (title == null) {
            event.reply("Nothing is playing.").queue()
            return
        }

        event.reply("Now playing: $title").queue()
    }

    private fun createPlaylist(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val name = event.getOption("name")!!.asString.trim()

        val box = playlistBoxes.getOrPut(guild.idLong) {
            PlaylistBox()
        }

        val created = box.createPlaylist(name)

        if (!created) {
            event.reply("Playlist already exists: $name").queue()
            return
        }

        event.reply("Playlist created: $name").queue()
    }

    private fun addBoxTrack(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        val guild = event.guild ?: return
        val playlistName = event.getOption("playlist")!!.asString.trim()
        val rawLink = event.getOption("link")!!.asString.trim()

        val query =
            if (rawLink.startsWith("http")) {
                rawLink
            } else {
                "ytsearch:$rawLink"
            }

        val box = playlistBoxes.getOrPut(guild.idLong) {
            PlaylistBox()
        }

        val link = lavalinkClient.getOrCreateLink(guild.idLong)

        link.loadItem(query).subscribe { result ->
            val track =
                when (result) {
                    is TrackLoaded -> result.track
                    is SearchResult -> result.tracks.firstOrNull()
                    is PlaylistLoaded -> result.tracks.firstOrNull()
                    else -> null
                }

            if (track == null) {
                event.hook.sendMessage("No track found.").queue()
                return@subscribe
            }

            val added = box.addTrack(playlistName, track)

            if (!added) {
                event.hook.sendMessage("Playlist not found: $playlistName").queue()
                return@subscribe
            }

            event.hook.sendMessage(
                "Added to $playlistName: ${track.info.title}"
            ).queue()
        }
    }

    private fun startBox(event: SlashCommandInteractionEvent) {
        event.deferReply().queue()

        val guild = event.guild ?: return
        val channel = event.member?.voiceState?.channel

        if (channel == null) {
            event.hook.sendMessage("Join voice channel first.").queue()
            return
        }

        val box = playlistBoxes[guild.idLong]

        if (box == null) {
            event.hook.sendMessage("Box is empty.").queue()
            return
        }

        val track = box.next()

        if (track == null) {
            event.hook.sendMessage("No tracks available in box.").queue()
            return
        }

        if (!guild.selfMember.voiceState!!.inAudioChannel()) {
            event.jda.directAudioController.connect(channel)
        }

        val link = lavalinkClient.getOrCreateLink(guild.idLong)
        val player = link.createOrUpdatePlayer()

        player.setVolume(100)
        player.setPaused(false)

        player.setTrack(track).subscribe {
            activePlayers.add(guild.idLong)
            currentTracks[guild.idLong] = track.info.title

            event.hook.sendMessage(
                "Box started. Now playing: ${track.info.title}"
            ).queue()
        }
    }

    private fun boxStatus(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val box = playlistBoxes[guild.idLong]

        if (box == null) {
            event.reply("Box is empty.").queue()
            return
        }

        val status = box.status()

        if (status.isEmpty()) {
            event.reply("Box is empty.").queue()
            return
        }

        event.reply("Box status:\n${status.joinToString("\n")}").queue()
    }

    private fun stopBox(event: SlashCommandInteractionEvent) {
        val guild = event.guild ?: return
        val box = playlistBoxes[guild.idLong]

        if (box == null) {
            event.reply("Box is already empty.").queue()
            return
        }

        box.clear()
        playlistBoxes.remove(guild.idLong)
        activePlayers.remove(guild.idLong)
        currentTracks.remove(guild.idLong)

        val player = lavalinkClient
            .getOrCreateLink(guild.idLong)
            .createOrUpdatePlayer()

        player.setTrack(null).subscribe {
            event.reply("Box stopped and cleared.").queue()
        }
    }
}