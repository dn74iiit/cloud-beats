# Local Spotify Downloader

A beautiful, local web application that allows you to easily download Spotify tracks, albums, and playlists directly to your PC. Built using Python, Flask, and `spotdl`.

## Features
- **Premium User Interface**: Aesthetic dark-mode UI with glassmorphism and glowing animations.
- **Easy Downloads**: Just paste the Spotify link and download instantly.
- **Background Processing**: Automates `spotdl` securely in the background.

## Prerequisites
- Python 3.7+
- FFmpeg (will be installed automatically via `spotdl` if missing)

## Installation & Setup

1. **Clone this repository** (or download the files):
   ```bash
   git clone https://github.com/dn74iiit/spotify-downloader
   cd spotify-downloader
   ```

2. **Install the required packages**:
   ```bash
   pip install flask spotdl
   ```

## How to Run the App

1. Open a terminal in the project directory.
2. Run the application:
   ```bash
   python app.py
   ```
3. Open your web browser and navigate to [http://127.0.0.1:5000](http://127.0.0.1:5000)

## How to Download a Song

1. Open the **Spotify app** or web player.
2. Find the track, album, or playlist you want to download.
3. Click the **Share** button and select **Copy Link**.
4. Open the Local Spotify Downloader web app (at `http://127.0.0.1:5000`).
5. **Paste the link** into the input box on the page.
6. Click **Download Now**.
7. Wait for the loading spinner to complete. Once finished, a success message will appear.
8. Check the `spotify_downloads` folder in your project directory to find your new downloaded songs!

---
*Note: This tool uses `spotdl` to download audio from YouTube matching the Spotify tracks. Please ensure you comply with the respective Terms of Service when using it.*
