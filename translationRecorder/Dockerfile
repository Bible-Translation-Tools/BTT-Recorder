# Base image with Android SDK installed
FROM budtmo/docker-android:emulator_14.0

# Install Python and other necessary tools
RUN sudo apt-get update && sudo apt-get install -y \
    python3 \
    python3-pip \
    wget \
    unzip

# Set working directory
WORKDIR /project/translationRecorder

COPY entrypoint.sh /entrypoint.sh
RUN sudo chmod +x /entrypoint.sh

# Make entrypoint.sh the default entrypoint
ENTRYPOINT ["/entrypoint.sh"]


