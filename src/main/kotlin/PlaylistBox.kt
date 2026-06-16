import dev.arbjerg.lavalink.client.player.Track

class PlaylistBox {
    private val playlists = mutableMapOf<String, MutableList<Track>>()
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

    fun addTrack(playlistName: String, track: Track): Boolean {
        val playlist = playlists[playlistName] ?: return false

        playlist.add(track)

        return true
    }

    fun next(): Track? {
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

    fun listPlaylists(): List<String> {
        return playlistOrder.map { name ->
            val size = playlists[name]?.size ?: 0
            "$name ($size tracks)"
        }
    }

    fun clear() {
        playlists.clear()
        playlistOrder.clear()
        currentPlaylist = 0
    }

    fun status(): List<String> {
        return playlistOrder.map { name ->
            val size = playlists[name]?.size ?: 0
            "$name ($size tracks)"
        }
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
}