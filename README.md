# Discord Music Bot

## Author

Created by **Oleksandr Nitsenko**.

GitHub: [Shkaf77](https://github.com/Shkaf77)
---

# Project Description

Discord Music Bot is a music bot built with Kotlin, JDA, and Lavalink.

The bot supports music playback from YouTube, queue management, and a custom Playlist Box system that allows multiple playlists to be played in round-robin order.

This project is currently under active development.

Current version: **Alpha v1**

---

# Features

## Music Playback
- Play tracks from YouTube
- Play using search queries
- Queue management
- Skip tracks
- Pause playback
- Resume playback
- Volume control
- Show currently playing track

## Playlist Box
- Create playlists inside the bot
- Add single tracks
- Import full YouTube playlists
- Round-robin playback
- Shuffle playlists
- Preview upcoming tracks
- Remove playlists
- Stop and clear playback

---

# Commands

## Basic Commands

```
/join
/leave
/play
/queue
/skip
/pause
/resume
/volume
/nowplaying
/help
```

## Playlist Box Commands

```
/createplaylist
/addboxtrack
/addplaylist
/startbox
/boxqueue
/boxstatus
/shuffleplaylist
/removeplaylist
/stopbox
```

---

# Technologies

- Kotlin
- JDA
- Lavalink
- YouTube Plugin
- yt-dlp
- Gradle

---

# Requirements

Install:
- Java 21
- Lavalink
- yt-dlp

```bash
# Install Java 21 using Homebrew
brew install openjdk@21

# Check Java version
java -version

# Install yt-dlp
brew install yt-dlp

# Check yt-dlp version
yt-dlp --version
```

Create `.env`

```env
DISCORD_TOKEN=
DISCORD_BOT_ID=
```

---

# Run Project

```bash
# Create Lavalink folder
mkdir lavalink
cd lavalink

# Download Lavalink.jar manually from:
# https://github.com/lavalink-devs/Lavalink/releases

# Create application.yml in the lavalink folder
touch application.yml

# Start Lavalink
java -jar Lavalink.jar
```

In another terminal, from the project root:

```bash
# Start the bot
./gradlew run

# Build the project
./gradlew build
```

---

# Known Limitations

- Some YouTube links may require authentication
- Playlist loading speed depends on YouTube responses
- Large playlists may load slower
- Public deployment is not configured yet

---

# Future Plans

- Docker deployment
- CI/CD
- Better UI responses
- Queue improvements
- Additional music sources
- Production deployment

---

## Project Status

This is a personal learning project created as part of my practice with Kotlin, Discord bots, Lavalink, and backend-style application development.