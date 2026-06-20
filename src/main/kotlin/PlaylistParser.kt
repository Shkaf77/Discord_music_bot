import java.io.File

fun extractPlaylistId(link: String): String? {
    val trimmed = link.trim()

    val playlistId =
        trimmed
            .substringAfter("list=", "")
            .substringBefore("&")
            .substringBefore("?")

    if (playlistId.isBlank()) {
        return null
    }

    return playlistId
}

fun buildPlaylistUrl(playlistId: String): String {
    return "https://www.youtube.com/playlist?list=$playlistId"
}

fun ytDlpCookieArgs(): List<String> {
    return if (File("cookies.txt").exists()) {
        listOf(
            "--cookies",
            "cookies.txt"
        )
    } else {
        emptyList()
    }
}

fun loadDirectAudioUrl(videoId: String): String? {
    val videoUrl = "https://www.youtube.com/watch?v=$videoId"

    val command =
        mutableListOf(
            "yt-dlp"
        )

    command.addAll(ytDlpCookieArgs())

    command.addAll(
        listOf(
            "-f",
            "ba",
            "-g",
            videoUrl
        )
    )

    val process =
        ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

    val output =
        process
            .inputStream
            .bufferedReader()
            .readLines()

    val exitCode =
        process.waitFor()

    if (exitCode != 0) {
        return null
    }

    return output.firstOrNull {
        it.startsWith("http")
    }
}

fun loadPlaylistTracks(playlistUrl: String): List<BoxTrack> {
    val command =
        mutableListOf(
            "yt-dlp"
        )

    command.addAll(ytDlpCookieArgs())

    command.addAll(
        listOf(
            "--flat-playlist",
            "--print",
            "%(id)s|||%(title)s",
            playlistUrl
        )
    )

    val process =
        ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

    val output =
        process
            .inputStream
            .bufferedReader()
            .readLines()

    val exitCode =
        process.waitFor()

    if (exitCode != 0) {
        return emptyList()
    }

    return output
        .map { it.trim() }
        .filter { it.contains("|||") }
        .mapNotNull { line ->
            val parts =
                line.split(
                    "|||",
                    limit = 2
                )

            if (parts.size != 2) {
                null
            } else {
                BoxTrack(
                    parts[0].trim(),
                    parts[1].trim()
                )
            }
        }
}
