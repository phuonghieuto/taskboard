#!/bin/bash

# Enable error handling
set -e

# Log file
LOG_FILE="start-services.dev.log"

# Redirect stdout and stderr to the log file
exec > >(tee -i $LOG_FILE)
exec 2>&1

# Disable exit on error
set +e

# Stop and remove containers, networks, images, and volumes
docker-compose -f docker-compose.dev.yaml down

# Build the services
docker-compose -f docker-compose.dev.yaml build

# Start the services in detached mode
docker-compose -f docker-compose.dev.yaml up -d

# Check the exit status of the last command
if [ $? -ne 0 ]; then
  echo "An error occurred. Check the log file for details: $LOG_FILE"
else
  echo "Services started successfully."
fi