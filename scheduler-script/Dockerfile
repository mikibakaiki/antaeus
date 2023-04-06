FROM python:3.9-slim-buster

# Install cron
RUN apt-get update && apt-get -y install cron

WORKDIR /app-scheduler

# setup python requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY scheduler_batch.py .
COPY scheduler_by_id.py .
COPY crontab /etc/cron.d/antaeus-cron


# Gives execution permission to the crontab file and python scripts
RUN chmod +x /etc/cron.d/antaeus-cron
RUN chmod +x scheduler_batch.py
RUN chmod +x scheduler_by_id.py

# Create the file to run the tail command
RUN touch /var/log/cron.log

# Applies the cron job
RUN crontab /etc/cron.d/antaeus-cron

# Run this on startup
CMD cron && tail -f /var/log/cron.log



