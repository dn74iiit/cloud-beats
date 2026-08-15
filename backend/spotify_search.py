import requests

def search_music(query, limit=10):
    """
    Searches the free iTunes API for music metadata.
    This completely bypasses the need for Spotify Premium developer accounts!
    """
    url = f"https://itunes.apple.com/search"
    params = {
        "term": query,
        "entity": "song",
        "limit": limit
    }
    
    result = requests.get(url, params=params)
    if result.status_code != 200:
        raise Exception("Failed to search music database")
        
    json_result = result.json()
    tracks = json_result.get('results', [])
    
    formatted_results = []
    for track in tracks:
        # iTunes returns a 100x100 image, we can usually hack the URL to get a larger one (e.g. 600x600)
        album_art = track.get('artworkUrl100', '').replace('100x100bb', '600x600bb')
        
        # We combine artist and title to create a perfect search string for spotdl
        search_query = f"{track.get('artistName')} - {track.get('trackName')}"
        
        formatted_results.append({
            "title": track.get('trackName'),
            "artist": track.get('artistName'),
            "album_art": album_art,
            "url": search_query # We pass this string instead of a spotify URL to spotdl
        })
        
    return formatted_results
