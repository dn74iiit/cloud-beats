import os
import requests

def get_access_token():
    client_id = os.environ.get("ONEDRIVE_CLIENT_ID")
    client_secret = os.environ.get("ONEDRIVE_CLIENT_SECRET")
    tenant_id = os.environ.get("ONEDRIVE_TENANT_ID", "common")
    refresh_token = os.environ.get("ONEDRIVE_REFRESH_TOKEN")

    if not all([client_id, client_secret, refresh_token]):
        raise ValueError("Missing OneDrive credentials in environment variables.")

    token_url = f"https://login.microsoftonline.com/{tenant_id}/oauth2/v2.0/token"
    
    payload = {
        'client_id': client_id,
        'client_secret': client_secret,
        'refresh_token': refresh_token,
        'grant_type': 'refresh_token'
    }

    response = requests.post(token_url, data=payload)
    if response.status_code == 200:
        return response.json().get('access_token')
    else:
        raise Exception(f"Failed to get access token: {response.text}")

def upload_file(local_path, destination_filename, onedrive_folder="/Songs"):
    """
    Uploads a local file to a specific folder in OneDrive.
    """
    access_token = get_access_token()
    
    # Graph API URL for uploading small/medium files (up to 4MB, though PUT works for slightly larger sometimes)
    # For very large files (e.g. video), an upload session is required. Audio files (<10-15MB) usually work fine with PUT.
    # Note: Microsoft recommends createUploadSession for files > 4MB. 
    # For a robust downloader, we will use a simple PUT request for simplicity, but if audio files exceed 4MB, it might fail.
    # Actually, OneDrive personal supports PUT up to 250MB, but best practice is 4MB. Let's just use PUT for now.
    
    url = f"https://graph.microsoft.com/v1.0/me/drive/root:{onedrive_folder}/{destination_filename}:/content"
    
    headers = {
        'Authorization': f'Bearer {access_token}',
        'Content-Type': 'audio/mpeg' # General audio mime
    }

    with open(local_path, 'rb') as f:
        file_data = f.read()
        
    response = requests.put(url, headers=headers, data=file_data)
    
    if response.status_code in (200, 201):
        return True, response.json()
    else:
        return False, response.text
