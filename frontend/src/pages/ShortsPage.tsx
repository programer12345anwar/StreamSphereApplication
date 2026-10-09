import { Layout } from "@/components/Layout";
import { useEffect, useMemo, useRef, useState } from "react";
import { getShorts } from "@/lib/api";
import { Link } from "react-router-dom";
import { Eye, Heart, MessageSquare, Play, Volume2, VolumeX } from "lucide-react";

interface ShortVideo {
  id: string;
  title: string;
  description?: string;
  videoLink?: string;
  thumbnailLink?: string;
  views?: number;
  channelName?: string;
  tags?: string[];
}

const ShortsPage = () => {
  const [shorts, setShorts] = useState<ShortVideo[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [muted, setMuted] = useState(true);
  const videoRefs = useRef<Record<string, HTMLVideoElement | null>>({});

  useEffect(() => {
    let mounted = true;

    const fetchShorts = async () => {
      setLoading(true);
      setError("");

      try {
        const feed = await getShorts(12);
        if (!mounted) return;
        setShorts(Array.isArray(feed) ? feed : []);
      } catch (err) {
        if (!mounted) return;
        setError(err instanceof Error ? err.message : "Failed to load shorts.");
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    fetchShorts();

    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    Object.entries(videoRefs.current).forEach(([id, video]) => {
      if (!video) return;
      video.muted = muted;
      if (id === "first") {
        video.play().catch(() => {});
      }
    });
  }, [muted]);

  const firstShortId = useMemo(() => shorts[0]?.id ?? null, [shorts]);

  const handlePlay = (videoId: string) => {
    Object.entries(videoRefs.current).forEach(([id, video]) => {
      if (!video) return;
      if (id === videoId) {
        video.play().catch(() => {});
      } else {
        video.pause();
      }
    });
  };

  return (
    <Layout>
      <div className="mx-auto max-w-md px-3 py-4">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-muted-foreground">Explore</p>
            <h1 className="text-2xl font-bold text-foreground">Shorts</h1>
          </div>
          <button
            type="button"
            onClick={() => setMuted((value) => !value)}
            className="rounded-full border border-border bg-background/80 p-2 text-foreground backdrop-blur-sm"
            aria-label={muted ? "Unmute shorts" : "Mute shorts"}
          >
            {muted ? <VolumeX className="h-4 w-4" /> : <Volume2 className="h-4 w-4" />}
          </button>
        </div>

        {loading && (
          <div className="space-y-4">
            {[...Array(3)].map((_, index) => (
              <div key={index} className="h-[72vh] animate-pulse rounded-[28px] bg-muted" />
            ))}
          </div>
        )}

        {!loading && error && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-600 dark:border-red-900/40 dark:bg-red-950/20 dark:text-red-300">
            {error}
          </div>
        )}

        {!loading && !error && shorts.length === 0 && (
          <div className="rounded-2xl border border-dashed border-border bg-muted/20 p-6 text-center text-sm text-muted-foreground">
            No shorts available right now.
          </div>
        )}

        {!loading && !error && shorts.length > 0 && (
          <div className="space-y-4">
            {shorts.map((short, index) => (
              <div key={short.id} className="relative overflow-hidden rounded-[28px] bg-black shadow-lg">
                <div className="relative h-[72vh] w-full overflow-hidden">
                  {short.videoLink ? (
                    <video
                      ref={(element) => {
                        videoRefs.current[short.id] = element;
                      }}
                      src={short.videoLink}
                      poster={short.thumbnailLink || undefined}
                      muted={muted}
                      playsInline
                      loop
                      autoPlay={index === 0 && firstShortId === short.id}
                      preload="metadata"
                      className="h-full w-full object-cover"
                      onClick={() => handlePlay(short.id)}
                      onLoadedMetadata={() => handlePlay(short.id)}
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center bg-gradient-to-br from-zinc-900 via-zinc-800 to-black text-white">
                      <div className="text-center">
                        <Play className="mx-auto mb-2 h-10 w-10" />
                        <p className="text-sm text-zinc-300">Video preview unavailable</p>
                      </div>
                    </div>
                  )}

                  <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/10 to-transparent" />

                  <div className="absolute inset-x-0 bottom-0 p-4 text-white">
                    <div className="mb-3 flex items-center gap-2 text-sm text-zinc-200">
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-white/15 font-semibold text-white">
                        {(short.channelName || "C").charAt(0).toUpperCase()}
                      </div>
                      <span className="font-medium">{short.channelName || "Unknown channel"}</span>
                    </div>

                    <Link to={`/watch/${short.id}`} className="block">
                      <h2 className="line-clamp-2 text-lg font-semibold text-white">{short.title}</h2>
                    </Link>

                    <div className="mt-2 flex flex-wrap items-center gap-3 text-xs text-zinc-200">
                      <span className="inline-flex items-center gap-1">
                        <Eye className="h-3.5 w-3.5" />
                        {short.views ? `${short.views.toLocaleString()} views` : "New"}
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <Heart className="h-3.5 w-3.5" />
                        Trending
                      </span>
                      <span className="inline-flex items-center gap-1">
                        <MessageSquare className="h-3.5 w-3.5" />
                        {short.tags?.length ? short.tags.length : 0}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default ShortsPage;
