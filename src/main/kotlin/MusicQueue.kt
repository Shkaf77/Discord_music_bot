import dev.arbjerg.lavalink.client.player.Track

class MusicQueue {
    private val queue = ArrayDeque<Track>()

    fun add(track: Track) {
        queue.addLast(track)
    }

    fun next(): Track? {
        return queue.removeFirstOrNull()
    }

    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }

    fun list(): List<Track> {
        return queue.toList()
    }

    fun clear() {
        queue.clear()
    }
}