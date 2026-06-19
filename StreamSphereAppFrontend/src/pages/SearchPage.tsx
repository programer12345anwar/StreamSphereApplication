import { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { Layout } from "@/components/Layout";
import { VideoCard } from "@/components/VideoCard";
import { getVideoFeed } from "@/lib/api";

const SearchPage = () => {
  const [searchParams] = useSearchParams();
  const query = (searchParams.get("q") || "").trim().toLowerCase();
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let mounted = true;

    const fetchVideos = async () => {
      setLoading(true);
      setError("");
      try {
        const feed = await getVideoFeed(100);
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

    fetchVideos();

    return () => {
      mounted = false;
    };
  }, []);

  const filtered = useMemo(() => {
    if (!query) {
      return videos;
    }

    return videos.filter((video) => {
      const title = (video.title || "").toLowerCase();
      const channel = (video.channelName || "").toLowerCase();
      return title.includes(query) || channel.includes(query);
    });
  }, [videos, query]);

  return (
    <Layout>
      <div className="p-6">
        <h1 className="text-xl font-semibold text-foreground">Search results</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {query ? `Query: ${query}` : "Showing all videos"}
        </p>

        {loading && <p className="mt-6 text-sm text-muted-foreground">Loading videos...</p>}
        {!loading && error && <p className="mt-6 text-sm text-red-500">{error}</p>}
        {!loading && !error && filtered.length === 0 && (
          <p className="mt-6 text-sm text-muted-foreground">No matching videos found.</p>
        )}

        {!loading && !error && filtered.length > 0 && (
          <div className="mt-6 grid grid-cols-1 gap-x-4 gap-y-8 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.map((video) => (
              <VideoCard key={video.id} video={video} />
            ))}
          </div>
        )}
      </div>
    </Layout>
  );
};

export default SearchPage;
