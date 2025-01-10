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

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Make entrypoint.sh the default entrypoint
ENTRYPOINT ["/entrypoint.sh"]


