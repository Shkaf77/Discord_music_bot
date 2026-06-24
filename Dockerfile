FROM eclipse-temurin:21-jdk

WORKDIR /app

RUN apt-get update \
    && apt-get install -y python3 python3-pip ffmpeg \
    && pip3 install --break-system-packages yt-dlp \
    && rm -rf /var/lib/apt/lists/*

COPY . .

RUN ./gradlew build --no-daemon

CMD ["./gradlew", "run", "--no-daemon"]