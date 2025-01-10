#!/bin/bash

# Mount points from the volumes
DOOR43_XML_PATH="/xml/door43.xml"
GITHUB_XML_PATH="/xml/github.xml"

# Rebuild chunks and move them to the proper location
python3 app/src/scripts/download_chunks.py
python3 app/src/scripts/get_obs.py
python3 app/src/scripts/get_tq.py

rm -r app/src/main/assets/chunks/
mv app/src/scripts/chunks/ app/src/main/assets/

# Copy XML files
cp "$DOOR43_XML_PATH" ./app/src/main/res/values/
cp "$GITHUB_XML_PATH" ./app/src/main/res/values/

# Download and unpack docs
wget -O docs.zip https://readthedocs.org/projects/btt-recorder/downloads/htmlzip/latest/ && \
    unzip docs.zip && \
    rm -r ./app/src/main/assets/btt-recorder.readthedocs.io && \
    mv btt-recorder-latest ./app/src/main/assets/btt-recorder.readthedocs.io

# Fix paths in the documentation
mv ./app/src/main/assets/btt-recorder.readthedocs.io/_static/ ./app/src/main/assets/btt-recorder.readthedocs.io/static/ && \
    mv ./app/src/main/assets/btt-recorder.readthedocs.io/_images/ ./app/src/main/assets/btt-recorder.readthedocs.io/images/ && \
    sed -i 's/_static/static/g' ./app/src/main/assets/btt-recorder.readthedocs.io/index.html && \
    sed -i 's/_images/images/g' ./app/src/main/assets/btt-recorder.readthedocs.io/index.html


./gradlew -PkeystorePath=/key/translationRecorderKey.jks \
              -PstorePass="$STORE_PASSWORD" \
              -PkeyPass="$KEY_PASSWORD" \
              assembleRelease