import { Layout } from "@/components/Layout";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { getVideoById, getVideoFeed } from "@/lib/api";
import { Download, MoreHorizontal, Share2, ThumbsDown, ThumbsUp } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";

const WatchPage = () => {
  const { videoId } = useParams();
  const [video, setVideo] = useState(null);
  const [suggested, setSuggested] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [liked, setLiked] = useState(false);

  useEffect(() => {
    let mounted = true;

    const fetchData = async () => {
      if (!videoId) {
        setError("Invalid video id.");
        setLoading(false);
        return;
      }

      setLoading(true);
      setError("");

      try {
        const [videoData, feed] = await Promise.all([getVideoById(videoId), getVideoFeed(20)]);
        if (!mounted) {
          return;
        }

        setVideo(videoData || null);
        setSuggested(Array.isArray(feed) ? feed.filter((item) => item.id !== videoId) : []);
      } catch (err) {
        if (mounted) {
          setError(err.message || "Failed to load video.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    fetchData();

    return () => {
      mounted = false;
    };
  }, [videoId]);

  const uploadedLabel = useMemo(() => {
    if (!video?.uploadedAt) {
      return "Recently";
    }
    return new Date(video.uploadedAt).toLocaleDateString();
  }, [video]);

  return (
    <Layout>
      {loading && <p className="p-6 text-sm text-muted-foreground">Loading video...</p>}
      {!loading && error && <p className="p-6 text-sm text-red-500">{error}</p>}

      {!loading && !error && video && (
        <div className="flex flex-col gap-6 p-6 lg:flex-row">
          <div className="min-w-0 flex-1">
            <div className="aspect-video w-full overflow-hidden rounded-xl bg-black">
              <video src={video.videoLink} controls className="h-full w-full" />
            </div>

            <h1 className="mt-4 text-xl font-semibold text-foreground">{video.title || "Untitled video"}</h1>

            <div className="mt-3 flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <Avatar className="h-10 w-10">
                  <AvatarFallback className="bg-primary/20 text-primary">
                    {(video.channelName || "C").charAt(0).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <p className="text-sm font-medium text-foreground">{video.channelName || "Unknown channel"}</p>
                  <p className="text-xs text-muted-foreground">{video.views || 0} views</p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <div className="flex items-center overflow-hidden rounded-full bg-secondary">
                  <button
                    onClick={() => setLiked((v) => !v)}
                    className={`flex items-center gap-2 px-4 py-2 text-sm transition-colors hover:bg-surface-hover ${
                      liked ? "text-primary" : "text-foreground"
                    }`}
                  >
                    <ThumbsUp className="h-4 w-4" />
                    Like
                  </button>
                  <div className="h-6 w-px bg-border" />
                  <button className="px-4 py-2 text-foreground hover:bg-surface-hover">
                    <ThumbsDown className="h-4 w-4" />
                  </button>
                </div>
                <Button variant="secondary" className="gap-2 rounded-full">
                  <Share2 className="h-4 w-4" /> Share
                </Button>
                <Button variant="secondary" className="gap-2 rounded-full">
                  <Download className="h-4 w-4" /> Download
                </Button>
                <Button variant="secondary" size="icon" className="rounded-full">
                  <MoreHorizontal className="h-4 w-4" />
                </Button>
              </div>
            </div>

            <div className="mt-4 rounded-xl bg-secondary p-3">
              <p className="text-sm font-medium text-foreground">
                {video.views || 0} views • {uploadedLabel}
              </p>
              <p className="mt-1 text-sm text-foreground/80">{video.description || "No description provided."}</p>
            </div>
          </div>

          <div className="w-full shrink-0 space-y-3 lg:w-96">
            <h3 className="text-sm font-medium text-foreground">Suggested</h3>
            {suggested.length === 0 && <p className="text-xs text-muted-foreground">No suggestions yet.</p>}
            {suggested.map((item) => (
              <Link key={item.id} to={`/watch/${item.id}`} className="group flex gap-2">
                <div className="relative aspect-video w-40 shrink-0 overflow-hidden rounded-lg bg-muted">
                  <div className="flex h-full items-center justify-center">
                    <svg viewBox="0 0 24 24" className="h-8 w-8 fill-muted-foreground/30">
                      <path d="M10 8l6 4-6 4V8z" />
                    </svg>
                  </div>
                </div>
                <div className="min-w-0">
                  <p className="line-clamp-2 text-sm font-medium text-foreground">{item.title}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{item.channelName}</p>
                  <p className="text-xs text-muted-foreground">{item.views || 0} views</p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </Layout>
  );
};

export default WatchPage;
