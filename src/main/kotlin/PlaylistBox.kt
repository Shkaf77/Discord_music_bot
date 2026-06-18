data class BoxTrack(
    val videoId: String,
    val title: String
)

class PlaylistBox {
    private val playlists = mutableMapOf<String, MutableList<BoxTrack>>()
    private val playlistOrder = mutableListOf<String>()
    private var currentPlaylist = 0

    fun createPlaylist(name: String): Boolean {
        if (playlists.containsKey(name)) {
            return false
        }

        playlists[name] = mutableListOf()
        playlistOrder.add(name)

        return true
    }

    fun addTrack(playlistName: String, videoId: String, title: String): Boolean {
        val playlist = playlists[playlistName] ?: return false

        playlist.add(
            BoxTrack(
                videoId,
                title
            )
        )

        return true
    }

    fun next(): BoxTrack? {
        if (playlistOrder.isEmpty()) {
            return null
        }

        var attempts = 0

        while (attempts < playlistOrder.size) {
            if (currentPlaylist >= playlistOrder.size) {
                currentPlaylist = 0
            }

            val playlistName = playlistOrder[currentPlaylist]
            val playlist = playlists[playlistName]

            if (playlist != null && playlist.isNotEmpty()) {
                val track = playlist.removeFirst()
                currentPlaylist++

                return track
            }

            currentPlaylist++
            attempts++
        }

        return null
    }

    fun status(): List<String> {
        return playlistOrder.map { name ->
            val size = playlists[name]?.size ?: 0
            "$name ($size tracks)"
        }
    }

    fun preview(limit: Int): List<String> {
        val result = mutableListOf<String>()

        if (playlistOrder.isEmpty()) {
            return result
        }

        val positions = mutableMapOf<String, Int>()

        playlistOrder.forEach { name ->
            positions[name] = 0
        }

        var playlistIndex = currentPlaylist
        var attemptsWithoutTrack = 0

        while (result.size < limit && attemptsWithoutTrack < playlistOrder.size) {
            if (playlistIndex >= playlistOrder.size) {
                playlistIndex = 0
            }

            val playlistName = playlistOrder[playlistIndex]
            val playlist = playlists[playlistName]
            val trackIndex = positions[playlistName] ?: 0

            if (playlist != null && trackIndex < playlist.size) {
                val track = playlist[trackIndex]
                result.add("$playlistName: ${track.title}")
                positions[playlistName] = trackIndex + 1
                attemptsWithoutTrack = 0
            } else {
                attemptsWithoutTrack++
            }

            playlistIndex++
        }

        return result
    }

    fun removePlaylist(name: String): Boolean {
        if (!playlists.containsKey(name)) {
            return false
        }

        playlists.remove(name)
        playlistOrder.remove(name)

        if (currentPlaylist >= playlistOrder.size) {
            currentPlaylist = 0
        }

        return true
    }

    fun clear() {
        playlists.clear()
        playlistOrder.clear()
        currentPlaylist = 0
    }

    fun shufflePlaylist(name: String): Boolean {
        val playlist = playlists[name] ?: return false

        playlist.shuffle()

        return true
    }
}