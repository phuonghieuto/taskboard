#!/bin/bash

# Check if a service name was provided
if [ $# -eq 0 ]; then
  echo "Error: No service name provided"
  echo "Usage: ./rebuild-single-service.sh <service-name>"
  echo "Available services: gateway, auth-service, task-service, discovery, db, etc."
  exit 1
fi

# Get the service name from the first argument
SERVICE_NAME=$1

LOG_FILE="rebuild-single-service.log"

echo "Rebuilding service: $SERVICE_NAME"
echo "Log will be saved to: $LOG_FILE"

# Redirect stdout and stderr to the log file
exec > >(tee -i $LOG_FILE)
exec 2>&1

# Rebuild the specified service
echo "Building $SERVICE_NAME..."
docker-compose -f docker-compose.yaml build $SERVICE_NAME

# Restart the specified service
echo "Restarting $SERVICE_NAME..."
docker-compose -f docker-compose.yaml up -d $SERVICE_NAME

# Check the exit status of the last command
if [ $? -ne 0 ]; then
  echo "An error occurred while rebuilding $SERVICE_NAME. Check the log file for details: $LOG_FILE"
  exit 1
else
  echo "Service $SERVICE_NAME rebuilt and restarted successfully."
fi