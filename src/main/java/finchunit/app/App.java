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
    private static final String accessToken = System.getenv("SPOTIFY_ACCESS_TOKEN");
    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setAccessToken(accessToken)
            .build();

    public static void main( String[] args )
    {
        String masterPlaylistId = "6ITZgA7oZvcu0KgkXfYDjk";

        List<PlaylistSimplified> pokerPlaylists = getPokerPlaylists();

        final List<PlaylistTrack> masterPlaylistTracks = getAllPlaylistTracks(masterPlaylistId);

        Map<String, Integer> duplicateOffenders = new HashMap<>();

        List<PlaylistTrack> playlistTracks;
        String userId;
        for (PlaylistSimplified playlist : pokerPlaylists) {
            printTitle(playlist.getName());
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
                }
                System.out.println(getTrackInfo(playlistTrack.getTrack()));
            }
        }
        printTitle("Duplicates");
        for (Map.Entry<String, Integer> dupesBy : duplicateOffenders.entrySet()) {
            System.out.println(String.format("%s: %d", dupesBy.getKey(), dupesBy.getValue()));
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
        System.out.println("=======================");
        System.out.println(title);
        System.out.println("=======================");
    }

    private static boolean isTrackInPlayList(PlaylistTrack playlistTrack, List<PlaylistTrack> playlistTracks)
    {
        for (PlaylistTrack exisitingPlaylistTrack : playlistTracks) {
            if (playlistTrack.getTrack().getId().equals(exisitingPlaylistTrack.getTrack().getId())) {
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
                .getListOfUsersPlaylists("thefinchmeister")
                .limit(50)
                .offset(45)
                .build();

        List<PlaylistSimplified> pokerPlaylists = new ArrayList<>();

        try {
            final Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfUsersPlaylistsRequest.execute();

            boolean isPokerPlaylist = false;
            for (PlaylistSimplified playlist : playlistSimplifiedPaging.getItems()) {
                if (playlist.getName().equals("Mistletokes")) { // TODO do not hard code first playlist
                    isPokerPlaylist = true;
                }

                if (isPokerPlaylist) {
                    pokerPlaylists.add(playlist);
                }

                if (playlist.getName().equals("The Bloat from Pokes")) {
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
