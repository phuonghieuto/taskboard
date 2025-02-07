# Log file
LOG_FILE="rebuild-single-service.log"

# Redirect stdout and stderr to the log file
exec > >(tee -i $LOG_FILE)
exec 2>&1

# Rebuild your service, put the name of the service you want to rebuild
docker-compose -f docker-compose.dev.yaml build game-service

# Restart your service
docker-compose -f docker-compose.dev.yaml up -d game-service

# Check the exit status of the last command
if [ $? -ne 0 ]; then
  echo "An error occurred. Check the log file for details: $LOG_FILE"
else
  echo "your service service rebuilt and restarted successfully."
fi