import subprocess
import sys
import os
import shutil
import uuid
import glob
from onedrive_uploader import upload_file

def check_dependencies():
    """Check if spotdl and ffmpeg are installed."""
    print("[*] Checking dependencies...")
    
    # Check if spotdl is installed via pip
    try:
        import spotdl
        print("  - spotdl is installed.")
    except ImportError:
        print("  - spotdl is not installed. Installing it now...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "spotdl"])
        print("  - spotdl installed successfully.")

    # Check for ffmpeg (required by spotdl to process audio files)
    ffmpeg_path_win = os.path.expanduser("~/.spotdl/ffmpeg.exe")
    ffmpeg_path_unix = os.path.expanduser("~/.spotdl/ffmpeg")
    
    if shutil.which("ffmpeg") is not None:
        print("  - ffmpeg is installed on system PATH.")
    elif os.path.exists(ffmpeg_path_win) or os.path.exists(ffmpeg_path_unix):
        print("  - ffmpeg is installed locally for spotdl.")
    else:
        print("  - ffmpeg not found.")
        print("    spotdl requires ffmpeg to process audio files.")
        print("    Attempting to download ffmpeg locally for spotdl...")
        try:
            p = subprocess.Popen([sys.executable, "-m", "spotdl", "--download-ffmpeg"], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            p.communicate(input='n\n')
            print("  - ffmpeg downloaded successfully.")
        except Exception:
            print("  [!] Failed to download ffmpeg automatically.")
            print("      Please install FFmpeg manually from https://ffmpeg.org/download.html")
            print("      and add it to your system PATH.")
            sys.exit(1)

def download_and_upload(url):
    """
    Downloads a single Spotify URL to a temp directory, uploads it to OneDrive, 
    and deletes the temp directory.
    """
    temp_dir = f"temp_dl_{uuid.uuid4().hex}"
    os.makedirs(temp_dir)
    print(f"\n[*] Processing URL: {url} in {temp_dir}")
    
    try:
        cmd = [
            sys.executable, "-m", "spotdl", "download", url,
            "--output", "{artists} - {title}",
            "--overwrite", "skip"
        ]
        
        # Download (blocking call)
        subprocess.check_call(cmd, cwd=temp_dir)
        
        # Find all downloaded files in the temp directory
        downloaded_files = glob.glob(f"{temp_dir}/*.*")
        if not downloaded_files:
            raise Exception("Download finished but no audio file was found.")
            
        success_count = 0
        for local_path in downloaded_files:
            filename = os.path.basename(local_path)
            # Skip hidden files or spotdl cache files if any
            if filename.startswith('.'):
                continue
                
            print(f"[*] Uploading {filename} to OneDrive...")
            success, result = upload_file(local_path, filename)
            
            if success:
                print(f"[*] Successfully uploaded {filename}.")
                success_count += 1
            else:
                print(f"[!] Failed to upload {filename}: {result}")
                
        if success_count > 0:
            return True, f"Successfully uploaded {success_count} file(s) to OneDrive."
        else:
            return False, "Failed to upload files to OneDrive."
            
    except subprocess.CalledProcessError as e:
        print(f"[!] Error downloading {url}: {e}")
        return False, f"Error downloading {url}: {e}"
    except Exception as e:
        print(f"[!] An unexpected error occurred: {e}")
        return False, f"An unexpected error occurred: {e}"
    finally:
        # Cleanup
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
            print(f"[*] Cleaned up temporary directory {temp_dir}.")

if __name__ == "__main__":
    print("=== Spotify Downloader (VPS Mode) ===")
    check_dependencies()
    print("Run app.py to start the web server.")
