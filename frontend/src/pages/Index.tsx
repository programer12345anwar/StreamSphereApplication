import { Layout } from "@/components/Layout";
import { VideoCard } from "@/components/VideoCard";
import { getVideoFeed, getTrendingFeed, getSubscriptionsFeed } from "@/lib/api";
import { useAuth } from "@/contexts/AuthContext";
import { useEffect, useMemo, useState } from "react";
import { cn } from "@/lib/utils";

const categories = ["All", "Trending", "Subscriptions"];

const Index = () => {
  const { user } = useAuth();
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activeCategory, setActiveCategory] = useState("All");

  useEffect(() => {
    let mounted = true;

    const fetchFeed = async () => {
      setLoading(true);
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
          setVideos(Array.isArray(feed) ? feed : []);
        }
      } catch (err) {
        if (mounted) {
          setError(err.message || "Failed to load videos.");
        }
      } finally {
        if (mounted) {
          setLoading(false);
        }
      }
    };

    fetchFeed();

    return () => {
      mounted = false;
    };
  }, [activeCategory, user]);

  const displayVideos = useMemo(() => {
    return videos;
  }, [videos]);

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
      {!loading && !error && displayVideos.length === 0 && (
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
