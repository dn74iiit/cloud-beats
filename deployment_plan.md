# Cloud Deployment Plan for Spotify Downloader Backend

This plan outlines the steps required to take your local Spotify downloader service (`C:\Users\dhanu\OneDrive\spotify_downloads\spotify-downloader`) and deploy it to a free cloud provider like **Render** so your app can access it from anywhere.

> [!NOTE]
> Since you requested just the plan without implementation, this is a step-by-step guide for you to follow.

## Phase 1: Prepare the Backend Code

Before deploying to the cloud, your Python script needs to be formatted as a standard web application.

1. **Web Framework:** Ensure your code uses a web framework like **Flask** or **FastAPI** to listen for HTTP requests, specifically the `POST /download` endpoint.
2. **Requirements File:** Create a `requirements.txt` file in your folder that lists all the libraries your script needs. For example:
   ```txt
   flask
   spotdl
   gunicorn
   ```
3. **Port Configuration:** Cloud providers assign ports dynamically. Ensure your app listens to the port provided by the environment variable `PORT` (e.g., `app.run(host='0.0.0.0', port=int(os.environ.get('PORT', 5000)))`).
4. **Push to GitHub:** Create a new GitHub repository for this backend code (separate from the Android app) and push your local files to it.

## Phase 2: Deploy to Render (Free Tier)

[Render](https://render.com/) is highly recommended because it offers a free tier for web services and connects directly to GitHub.

1. **Sign Up:** Go to Render.com and sign up using your GitHub account.
2. **Create New Web Service:** Click "New" > "Web Service".
3. **Connect Repository:** Select the GitHub repository you just created for your backend code.
4. **Configure Settings:**
   - **Environment:** Python 3
   - **Build Command:** `pip install -r requirements.txt`
   - **Start Command:** `gunicorn app:app` (Replace `app:app` with your actual filename and flask instance name, e.g., `main:app`).
5. **Deploy:** Click "Create Web Service". Render will build and deploy your app.
6. **Get URL:** Once the deployment is live, copy your public URL (it will look something like `https://cloud-beats-downloader.onrender.com`).

> [!WARNING]
> Free tier services on Render "spin down" after 15 minutes of inactivity. This means the very first time you request a download after a period of inactivity, the request might take up to 30-50 seconds while the server wakes up.

## Phase 3: Update the Android App

Once your service is live on the internet, you simply need to point your Android app to it.

1. Open [`SpotifyService.kt`](file:///c:/Users/dhanu/Documents/AUDIOAI/cloud-beatsxx/app/src/main/java/com/cloudbeats/app/data/remote/SpotifyService.kt) in your Cloud Beats project.
2. Locate line 15:
   ```kotlin
   private const val BACKEND_URL = "http://10.0.2.2:5000"
   ```
3. Replace that string with your new Render URL:
   ```kotlin
   private const val BACKEND_URL = "https://cloud-beats-downloader.onrender.com"
   ```
4. Build the final Release version of your app and upload it to your GitHub releases!
