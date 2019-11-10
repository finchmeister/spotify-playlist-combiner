package finchunit.app;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.model_objects.specification.Track;
import com.wrapper.spotify.requests.data.playlists.GetListOfUsersPlaylistsRequest;

import java.io.IOException;
import java.util.*;

public class App
{
    private static final String FIRST_PLAYLIST_TO_INCLUDE = "Jays Revokes from Pokes";
    private static final String LAST_PLAYLIST_TO_INCLUDE = "Soaked in Smoke";
    private static final String USER_NAME = "thefinchmeister";

    private static final String PLAYLIST_ID_TO_GO_ON_THE_BEAST = "3OsBd103JuT9XxOtyfwmfO";
    private static final String PLAYLIST_ID_THE_BEAST = "0fl9tBdvQbObUre4IG8cXy";
    private static final String PLAYLIST_ID_THE_BEAST_COPY = "6ITZgA7oZvcu0KgkXfYDjk";

    private static final String SPOTIFY_ACCESS_TOKEN = System.getenv("SPOTIFY_ACCESS_TOKEN");
    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setAccessToken(SPOTIFY_ACCESS_TOKEN)
            .build();

    public static void main( String[] args )
    {
        String masterPlaylistId = PLAYLIST_ID_THE_BEAST_COPY;

        List<PlaylistSimplified> pokerPlaylists = getPokerPlaylists();

        final List<PlaylistTrack> masterPlaylistTracks = getAllPlaylistTracks(masterPlaylistId);

        Map<String, Integer> duplicateOffenders = new HashMap<>();

        List<PlaylistTrack> playlistTracks;
        List<PlaylistTrack> playlistTracksToAdd = new ArrayList<>();
        String userId;
        for (PlaylistSimplified playlist : pokerPlaylists) {
            printTitle(playlist.getName() + " - " + playlist.getId());
            playlistTracks = getAllPlaylistTracks(playlist.getId());
            for (PlaylistTrack playlistTrack : playlistTracks) {
                if (isTrackInPlayList(playlistTrack, masterPlaylistTracks)) {
                    // Dupe
                    userId = playlistTrack.getAddedBy().getId();
                    System.out.print(String.format("Dupe %s", userId));
                    int count = duplicateOffenders.containsKey(userId) ? duplicateOffenders.get(userId) : 0;
                    duplicateOffenders.put(userId, count + 1);
                } else {
                    // Add to master playlist
                    System.out.print("Add to playlist");
                    playlistTracksToAdd.add(playlistTrack);
                }
                System.out.println(getTrackInfo(playlistTrack.getTrack()));
            }
        }
        printTitle("Duplicates");
        for (Map.Entry<String, Integer> dupesBy : duplicateOffenders.entrySet()) {
            System.out.println(String.format("%s: %d", dupesBy.getKey(), dupesBy.getValue()));
        }

        String newPlaylistId = PLAYLIST_ID_TO_GO_ON_THE_BEAST;

        addPlaylistTracks(newPlaylistId, playlistTracksToAdd);
    }

    private static void addPlaylistTracks(String playlistId, List<PlaylistTrack> playlistTracks)
    {
        System.out.println("About to add " + playlistTracks.size() + " tracks to the playlist");
        String[] urisToAddToPlaylist = new String[playlistTracks.size()];
        for (int i = 0; i < playlistTracks.size(); i++) {
            urisToAddToPlaylist[i] = playlistTracks.get(i).getTrack().getUri();
        }

        try {
            int chunk = 50;
            int currentChunkSize;
            for (int i = 0; i < urisToAddToPlaylist.length; i += chunk){
                spotifyApi.addTracksToPlaylist(playlistId, Arrays.copyOfRange(urisToAddToPlaylist, i, Math.min(urisToAddToPlaylist.length, i+chunk))).build().execute();
                currentChunkSize = Math.min(urisToAddToPlaylist.length, i+chunk) - i;
                System.out.println("Adding " + currentChunkSize + " tracks");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static String getTrackInfo(Track track)
    {
        return String.format(
                ": %s - %s",
                track.getName(),
                track.getArtists()[0].getName()
        );
    }

    private static void printTitle(String title)
    {
        String banner = new String(new char[title.length()]).replace("\0", "=");
        System.out.println(banner);
        System.out.println(title);
        System.out.println(banner);
    }

    private static boolean isTrackInPlayList(PlaylistTrack playlistTrack, List<PlaylistTrack> existingPlaylistTracks)
    {
        for (PlaylistTrack existingPlaylistTrack : existingPlaylistTracks) {
            if (playlistTrack.getTrack().getId().equals(existingPlaylistTrack.getTrack().getId())) {
                return true;
            }
            // Consider tracks with the same name and artist as dupes
            if (getTrackInfo(playlistTrack.getTrack()).equals(getTrackInfo(existingPlaylistTrack.getTrack()))) {
                return true;
            }
        }

        return false;
    }

    private static List<PlaylistTrack> getAllPlaylistTracks(String playlistId)
    {
        int offset = 0;
        int pageSize = 100;
        Paging<PlaylistTrack> pagedPlaylistTracks;
        List<PlaylistTrack> playlistTracks = new ArrayList<>();

        try {
            do {
                pagedPlaylistTracks = spotifyApi
                        .getPlaylistsTracks(playlistId)
                        .limit(pageSize)
                        .offset(offset)
                        .build()
                        .execute();

                for (PlaylistTrack playlistTrack : pagedPlaylistTracks.getItems()) {
                    if (playlistTrack.getTrack().getId() == null) {
                        continue;
                    }
                    playlistTracks.add(playlistTrack);
                }
                offset += pageSize;
            } while (pagedPlaylistTracks.getNext() != null);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }

        return playlistTracks;
    }

    private static List<PlaylistSimplified> getPokerPlaylists() {

        // TODO do not hard code limit & offset
        final GetListOfUsersPlaylistsRequest getListOfUsersPlaylistsRequest = spotifyApi
                .getListOfUsersPlaylists(USER_NAME)
                .limit(50)
                .offset(45)
                .build();

        List<PlaylistSimplified> pokerPlaylists = new ArrayList<>();

        try {
            final Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfUsersPlaylistsRequest.execute();

            boolean isPokerPlaylist = false;
            for (PlaylistSimplified playlist : playlistSimplifiedPaging.getItems()) {
                if (playlist.getName().equals(FIRST_PLAYLIST_TO_INCLUDE)) {
                    isPokerPlaylist = true;
                }

                if (isPokerPlaylist) {
                    pokerPlaylists.add(playlist);
                }

                if (playlist.getName().equals(LAST_PLAYLIST_TO_INCLUDE)) {
                    break;
                }
            }

        } catch (IOException | SpotifyWebApiException e) {
            System.out.println("Error: " + e.getMessage());
        }

        Collections.reverse(pokerPlaylists);

        return pokerPlaylists;
    }
}
