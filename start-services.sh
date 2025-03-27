#!/bin/bash

# Enable error handling
set -e

# Log file
LOG_FILE="start-services.log"

# Redirect stdout and stderr to the log file
exec > >(tee -i $LOG_FILE)
exec 2>&1

# Stop all containers and remove volumes
echo "Stopping all containers..."
docker-compose -f docker-compose-dev.yaml -f docker-compose-monitoring.yaml down

# Ensure the network is removed if it exists
echo "Removing network if it exists..."
docker network rm microservices-network 2>/dev/null || true

# Build the services
echo "Building services..."
docker-compose -f docker-compose-dev.yaml -f docker-compose-monitoring.yaml build

# Start the services in detached mode
echo "Starting services..."
docker-compose -f docker-compose-dev.yaml -f docker-compose-monitoring.yaml up -d

# Check the exit status of the last command
if [ $? -ne 0 ]; then
  echo "An error occurred. Check the log file for details: $LOG_FILE"
else
  echo "Services started successfully."
fi