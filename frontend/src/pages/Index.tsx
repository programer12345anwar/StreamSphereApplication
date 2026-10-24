import { Layout } from "@/components/Layout";
import { VideoCard } from "@/components/VideoCard";
import { getVideoFeed, getTrendingFeed, getSubscriptionsFeed } from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";
import { useEffect, useMemo, useState } from "react";
import { useLocation } from "react-router-dom";
import { cn } from "@/lib/utils";

const categories = ["All", "Trending", "Subscriptions"];
const UPLOADED_VIDEO_POLL_INTERVAL_MS = 3000;
const UPLOADED_VIDEO_POLL_TIMEOUT_MS = 60000;

const Index = () => {
  const { user } = useAuth();
  const location = useLocation();
  const uploadedVideoId = location.state?.uploadedVideoId;
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");
  const [uploadProcessingTimedOut, setUploadProcessingTimedOut] = useState(false);
  const [refreshAttempt, setRefreshAttempt] = useState(0);

  useEffect(() => {
    let mounted = true;
    let pollTimeout;

    const fetchFeed = async (showLoading) => {
      if (showLoading) {
        setLoading(true);
      }
      setError("");
      try {
        let feed = [];
        if (activeCategory === "Trending") {
          feed = await getTrendingFeed(40);
        } else if (activeCategory === "Subscriptions") {
          if (!user) {
            setError("Please log in to see subscriptions.");
            setLoading(false);
            return;
          }
          feed = await getSubscriptionsFeed(user.id, 40);
        } else {
          feed = await getVideoFeed(40);
        }
        
        if (mounted) {
          const result = Array.isArray(feed) ? feed : [];
          setVideos(result);
          return result.some((video) => video.id === uploadedVideoId);
        }
      } catch (err) {
        if (mounted) {
          setError(err.message || "Failed to load videos.");
        }
      } finally {
        if (mounted && showLoading) {
          setLoading(false);
        }
      }
      return false;
    };

    setLoading(true);
    setError("");
    setUploadProcessingTimedOut(false);

    const monitorUploadedVideo = async () => {
      const videoIsVisible = await fetchFeed(false);
      if (!mounted || videoIsVisible) {
        return;
      }

      if (Date.now() >= pollDeadline) {
        setUploadProcessingTimedOut(true);
        return;
      }

      pollTimeout = window.setTimeout(monitorUploadedVideo, UPLOADED_VIDEO_POLL_INTERVAL_MS);
    };

    const pollDeadline = Date.now() + UPLOADED_VIDEO_POLL_TIMEOUT_MS;
    fetchFeed(true).then((videoIsVisible) => {
      if (
        mounted &&
        uploadedVideoId &&
        activeCategory === "All" &&
        !videoIsVisible
      ) {
        pollTimeout = window.setTimeout(monitorUploadedVideo, UPLOADED_VIDEO_POLL_INTERVAL_MS);
      }
    });

    return () => {
      mounted = false;
      window.clearTimeout(pollTimeout);
    };
  }, [activeCategory, refreshAttempt, uploadedVideoId, user]);

  const displayVideos = useMemo(() => {
    return videos;
  }, [videos]);
  const uploadedVideoIsVisible = displayVideos.some((video) => video.id === uploadedVideoId);
  const monitoringUploadedVideo =
    Boolean(uploadedVideoId) && activeCategory === "All" && !uploadedVideoIsVisible;

  return (
    <Layout>
      <div className="sticky top-14 z-30 flex gap-3 overflow-x-auto bg-background px-6 py-3 scrollbar-hide">
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setActiveCategory(cat)}
            className={cn(
              "shrink-0 rounded-lg px-3 py-1.5 text-sm font-medium transition-colors",
              activeCategory === cat
                ? "bg-chip-active text-chip-active-foreground"
                : "bg-chip text-foreground hover:bg-surface-hover",
            )}
          >
            {cat}
          </button>
        ))}
      </div>

      {loading && <p className="p-6 text-sm text-muted-foreground">Loading videos...</p>}
      {!loading && error && <p className="p-6 text-sm text-red-500">{error}</p>}
      {!loading && !error && monitoringUploadedVideo && (
        <div className="px-6 pb-2 text-sm text-muted-foreground">
          {uploadProcessingTimedOut ? (
            <>
              <p>Your video is taking longer than expected to process. It will appear here once it is published.</p>
              <button
                type="button"
                className="mt-2 underline underline-offset-4"
                onClick={() => setRefreshAttempt((attempt) => attempt + 1)}
              >
                Check again
              </button>
            </>
          ) : (
            <p role="status">Your video is processing. Home will refresh automatically.</p>
          )}
        </div>
      )}
      {!loading && !error && displayVideos.length === 0 && !monitoringUploadedVideo && (
        <p className="p-6 text-sm text-muted-foreground">No videos found yet. Upload your first video.</p>
      )}

      {!loading && !error && displayVideos.length > 0 && (
        <div className="grid grid-cols-1 gap-x-4 gap-y-8 p-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {displayVideos.map((video) => (
            <VideoCard key={video.id} video={video} />
          ))}
        </div>
      )}
    </Layout>
  );
};

export default Index;
