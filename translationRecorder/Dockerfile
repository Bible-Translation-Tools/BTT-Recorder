# Base image with Android SDK installed
FROM runmymind/docker-android-sdk:ubuntu-standalone

# Install Python and other necessary tools
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /project

COPY . .

# Rebuild chunks and move them to the proper location
RUN python3 app/src/scripts/download_chunks.py
RUN python3 app/src/scripts/get_obs.py
RUN python3 app/src/scripts/get_tq.py
RUN rm -r app/src/main/assets/chunks/ && \
    mv app/src/scripts/chunks/ app/src/main/assets/

# Copy XML files
RUN cp "$DOOR43_XML" ./app/src/main/res/values/ && \
    cp "$GITHUB_XML" ./app/src/main/res/values/

# Download and unpack docs
RUN wget -O docs.zip https://readthedocs.org/projects/btt-recorder/downloads/htmlzip/latest/ && \
    unzip docs.zip && \
    rm -r ./app/src/main/assets/btt-recorder.readthedocs.io && \
    mv btt-recorder-latest ./app/src/main/assets/btt-recorder.readthedocs.io

# Fix paths in the documentation
RUN mv ./app/src/main/assets/btt-recorder.readthedocs.io/_static/ ./app/src/main/assets/btt-recorder.readthedocs.io/static/ && \
    mv ./app/src/main/assets/btt-recorder.readthedocs.io/_images/ ./app/src/main/assets/btt-recorder.readthedocs.io/images/ && \
    sed -i 's/_static/static/g' ./app/src/main/assets/btt-recorder.readthedocs.io/index.html && \
    sed -i 's/_images/images/g' ./app/src/main/assets/btt-recorder.readthedocs.io/index.html

# Build the signed APK
RUN ./gradlew -PkeystorePath=/key/translationRecorderKey.jks \
              -PstorePass="$STORE_PASSWORD" \
              -PkeyPass="$KEY_PASSWORD" \
              assembleRelease

