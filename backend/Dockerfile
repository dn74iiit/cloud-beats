FROM python:3.10-slim

# Install ffmpeg (required by spotdl)
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of your backend code
COPY . .

# Render dynamically assigns a port using the PORT environment variable.
# We start gunicorn and tell it to listen on that port.
CMD gunicorn app:app --bind 0.0.0.0:$PORT --timeout 100
