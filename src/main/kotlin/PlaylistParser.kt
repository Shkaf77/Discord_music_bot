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

fun loadPlaylistVideoIds(playlistUrl: String): List<String> {
    val process =
        ProcessBuilder(
            "yt-dlp",
            "--cookies",
            "cookies.txt",
            "--flat-playlist",
            "--print",
            "%(id)s",
            playlistUrl
        )
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
        .filter { it.isNotBlank() && !it.startsWith("[") && !it.startsWith("WARNING") }
}

fun loadDirectAudioUrl(videoId: String): String? {
    val videoUrl = "https://www.youtube.com/watch?v=$videoId"

    val process =
        ProcessBuilder(
            "yt-dlp",
            "--cookies",
            "cookies.txt",
            "-f",
            "ba",
            "-g",
            videoUrl
        )
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
    val process =
        ProcessBuilder(
            "yt-dlp",
            "--flat-playlist",
            "--print",
            "%(id)s|||%(title)s",
            playlistUrl
        )
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
            val parts = line.split("|||", limit = 2)

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