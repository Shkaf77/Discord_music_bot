import io.github.cdimascio.dotenv.Dotenv
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent

fun main() {
    val env = Dotenv.load()

    val discordToken =
        env["DISCORD_TOKEN"]
            ?: error("DISCORD_TOKEN is missing")

    JDABuilder.createDefault(discordToken)
        .enableIntents(
            GatewayIntent.GUILD_VOICE_STATES
        )
        .build()

    println("Bot started")
}