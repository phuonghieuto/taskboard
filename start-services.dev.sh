#!/bin/bash

# Enable error handling
set -e

# Log file
LOG_FILE="start-services.dev.log"

# Redirect stdout and stderr to the log file
exec > >(tee -i $LOG_FILE)
exec 2>&1

# Stop all containers and remove volumes
echo "Stopping all containers..."
docker-compose -f docker-compose.dev.yaml down

# # Remove the postgres volume explicitly
# echo "Removing PostgreSQL volume..."
# docker volume rm postgres_data || true

# # Prune unused volumes
# echo "Pruning unused volumes..."
# docker volume prune -f

# Build the services
echo "Building services..."
docker-compose -f docker-compose.dev.yaml build

# Start the services in detached mode
echo "Starting services..."
docker-compose -f docker-compose.dev.yaml up -d

# Check the exit status of the last command
if [ $? -ne 0 ]; then
  echo "An error occurred. Check the log file for details: $LOG_FILE"
else
  echo "Services started successfully."
fi