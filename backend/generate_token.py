import os
import webbrowser
import urllib.parse
from http.server import HTTPServer, BaseHTTPRequestHandler
import requests

# Fill these in after creating your Azure App
CLIENT_ID = "PASTE_CLIENT_ID_HERE"
CLIENT_SECRET = "PASTE_CLIENT_SECRET_HERE"

REDIRECT_URI = "http://localhost:8080"
SCOPES = ["offline_access", "Files.ReadWrite.All", "User.Read"]

auth_code = None

class OAuthCallbackHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        global auth_code
        self.send_response(200)
        self.send_header('Content-type', 'text/html')
        self.end_headers()
        
        query_components = urllib.parse.parse_qs(urllib.parse.urlparse(self.path).query)
        
        if 'code' in query_components:
            auth_code = query_components['code'][0]
            self.wfile.write(b"<html><body><h1>Success!</h1><p>You can close this window and return to your terminal.</p></body></html>")
        else:
            self.wfile.write(b"<html><body><h1>Error</h1><p>Failed to get authorization code.</p></body></html>")
            
    def log_message(self, format, *args):
        pass # Suppress HTTP logs

def main():
    if CLIENT_ID == "PASTE_CLIENT_ID_HERE" or CLIENT_SECRET == "PASTE_CLIENT_SECRET_HERE":
        print("ERROR: Please open generate_token.py and paste your CLIENT_ID and CLIENT_SECRET first!")
        return

    # 1. Generate Login URL
    auth_url = f"https://login.microsoftonline.com/common/oauth2/v2.0/authorize" \
               f"?client_id={CLIENT_ID}" \
               f"&response_type=code" \
               f"&redirect_uri={urllib.parse.quote(REDIRECT_URI)}" \
               f"&response_mode=query" \
               f"&scope={urllib.parse.quote(' '.join(SCOPES))}"

    print("[*] Opening browser to log into Microsoft...")
    webbrowser.open(auth_url)

    # 2. Start local server to catch the redirect
    print("[*] Waiting for authorization...")
    server = HTTPServer(('localhost', 8080), OAuthCallbackHandler)
    
    while auth_code is None:
        server.handle_request()

    # 3. Exchange code for Refresh Token
    print("[*] Authorization received! Requesting Refresh Token...")
    token_url = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
    payload = {
        'client_id': CLIENT_ID,
        'client_secret': CLIENT_SECRET,
        'code': auth_code,
        'redirect_uri': REDIRECT_URI,
        'grant_type': 'authorization_code'
    }
    
    response = requests.post(token_url, data=payload)
    
    if response.status_code == 200:
        data = response.json()
        print("\n" + "="*50)
        print("SUCCESS! Here is what you need to paste into your backend/.env file:")
        print("="*50)
        print(f"ONEDRIVE_CLIENT_ID={CLIENT_ID}")
        print(f"ONEDRIVE_CLIENT_SECRET={CLIENT_SECRET}")
        print("ONEDRIVE_TENANT_ID=common")
        print(f"ONEDRIVE_REFRESH_TOKEN={data.get('refresh_token')}")
        print("="*50 + "\n")
    else:
        print(f"\n[!] Error getting token: {response.text}")

if __name__ == "__main__":
    main()
