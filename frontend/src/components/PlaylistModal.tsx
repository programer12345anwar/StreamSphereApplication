import { useEffect, useState } from "react";
import {
  addVideoToPlaylist,
  createPlaylist,
  getChannelPlaylists,
} from "@/lib/api";

interface Playlist {
  id: string | number;
  name: string;
}

interface PlaylistModalProps {
  videoId: string;
  channelId: string;
  isOpen: boolean;
  onClose: () => void;
}

export function PlaylistModal({
  videoId,
  channelId,
  isOpen,
  onClose,
}: PlaylistModalProps) {
  const [playlists, setPlaylists] = useState<Playlist[]>([]);
  const [playlistName, setPlaylistName] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!isOpen || !channelId) return;

    let cancelled = false;

    async function loadPlaylists() {
      setLoading(true);
      setError("");
      setMessage("");

      try {
        const response = await getChannelPlaylists(channelId);
        const data = Array.isArray(response)
          ? response
          : response?.data ?? response?.content ?? response?.playlists ?? [];

        if (!cancelled) {
          setPlaylists(Array.isArray(data) ? data : []);
        }
      } catch {
        if (!cancelled) {
          setError("Could not load playlists. Please try again.");
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void loadPlaylists();

    return () => {
      cancelled = true;
    };
  }, [isOpen, channelId]);

  if (!isOpen) return null;

  async function handleCreatePlaylist() {
    const name = playlistName.trim();

    if (!name || !channelId) {
      setError("Enter a playlist name.");
      return;
    }

    setSaving(true);
    setError("");
    setMessage("");

    try {
      await createPlaylist(channelId, name);
      setPlaylistName("");

      const response = await getChannelPlaylists(channelId);
      const data = Array.isArray(response)
        ? response
        : response?.data ?? response?.content ?? response?.playlists ?? [];

      setPlaylists(Array.isArray(data) ? data : []);
      setMessage("Playlist created successfully.");
    } catch {
      setError("Could not create playlist. Please try again.");
    } finally {
      setSaving(false);
    }
  }

  async function handleAddVideo(playlistId: string | number) {
    setSaving(true);
    setError("");
    setMessage("");

    try {
      await addVideoToPlaylist(String(playlistId), videoId);
      setMessage("Video added to playlist successfully.");
    } catch {
      setError("Could not add video to playlist.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !saving) onClose();
      }}
    >
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="playlist-modal-title"
        className="w-full max-w-md rounded-xl border bg-background p-6 text-foreground shadow-xl"
      >
        <div className="mb-5 flex items-center justify-between gap-4">
          <h2 id="playlist-modal-title" className="text-xl font-semibold">
            Save to playlist
          </h2>
          <button
            type="button"
            onClick={onClose}
            disabled={saving}
            aria-label="Close"
            className="rounded px-3 py-1 text-xl hover:bg-muted disabled:opacity-50"
          >
            ×
          </button>
        </div>

        {loading ? (
          <p role="status" className="py-4 text-sm text-muted-foreground">
            Loading playlists...
          </p>
        ) : (
          <div className="max-h-64 space-y-2 overflow-y-auto">
            {playlists.map((playlist) => (
              <div
                key={playlist.id}
                className="flex items-center justify-between gap-3 rounded-lg border p-3"
              >
                <span className="break-words">{playlist.name}</span>
                <button
                  type="button"
                  disabled={saving}
                  onClick={() => void handleAddVideo(playlist.id)}
                  className="shrink-0 rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground disabled:opacity-50"
                >
                  Add
                </button>
              </div>
            ))}

            {!playlists.length && (
              <p className="py-3 text-sm text-muted-foreground">
                No playlists found. Create one below.
              </p>
            )}
          </div>
        )}

        <div className="mt-5 flex gap-2">
          <input
            value={playlistName}
            onChange={(event) => setPlaylistName(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !saving) {
                void handleCreatePlaylist();
              }
            }}
            placeholder="New playlist name"
            maxLength={100}
            className="min-w-0 flex-1 rounded-md border bg-background px-3 py-2 text-sm"
          />
          <button
            type="button"
            disabled={saving || !playlistName.trim() || !channelId}
            onClick={() => void handleCreatePlaylist()}
            className="rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground disabled:opacity-50"
          >
            Create
          </button>
        </div>

        {error && (
          <p role="alert" className="mt-3 text-sm text-destructive">
            {error}
          </p>
        )}

        {message && (
          <p role="status" className="mt-3 text-sm">
            {message}
          </p>
        )}
      </section>
    </div>
  );
}
