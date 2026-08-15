from flask import Flask, request, jsonify
from dotenv import load_dotenv
import os
from spotify_downloader import download_and_upload, check_dependencies
from spotify_search import search_music

# Load environment variables from .env file
load_dotenv()

app = Flask(__name__)

# Run checks on startup
try:
    check_dependencies()
except SystemExit:
    print("Dependencies failed. Please fix before running app.")

@app.route('/')
def index():
    return jsonify({"status": "Cloud Beats Backend is running."})

@app.route('/api/search', methods=['GET'])
def search():
    query = request.args.get('q')
    if not query:
        return jsonify({'success': False, 'message': 'No search query provided'}), 400
        
    try:
        results = search_music(query)
        return jsonify({'success': True, 'results': results}), 200
    except Exception as e:
        return jsonify({'success': False, 'message': str(e)}), 500

@app.route('/api/download', methods=['POST'])
def download():
    data = request.json
    if not data:
        return jsonify({'success': False, 'message': 'Invalid JSON body'}), 400
        
    url = data.get('url')
    if not url:
        return jsonify({'success': False, 'message': 'No URL provided'}), 400
        
    # Process download and upload synchronously
    success, message = download_and_upload(url)
    
    if success:
        return jsonify({'success': True, 'message': message}), 200
    else:
        return jsonify({'success': False, 'message': message}), 500

if __name__ == '__main__':
    # Default to 8000 for VPS setup
    port = int(os.environ.get('PORT', 8000))
    app.run(debug=False, host='0.0.0.0', port=port)
