import { Layout } from "@/components/Layout";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import CommentsSection from "@/components/CommentsSection";
import { useAuth } from "@/contexts/AuthContext";
import { checkSubscriptionStatus, subscribeToChannel, unsubscribeFromChannel, toggleLikeVideo, recordVideoView, getVideoById, getVideoFeed, getResumeTime, updateResumeTime } from "@/lib/api";
import { toast } from "@/hooks/use-toast";
import { Download, Share2, ThumbsDown, ThumbsUp } from "lucide-react";
import { useEffect, useMemo, useState, useRef } from "react";
import { Link, useParams } from "react-router-dom";
import { PlaylistModal } from "@/components/PlaylistModal";
import { ListPlus } from "lucide-react";

const WatchPage = () => {
  const { videoId } = useParams();
  const { user } = useAuth();
  const [video, setVideo] = useState(null);
  const [suggested, setSuggested] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [liked, setLiked] = useState(false);
  const [disliked, setDisliked] = useState(false);
  const [subscribed, setSubscribed] = useState(false);
  const [subLoading, setSubLoading] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [initialResumeTime, setInitialResumeTime] = useState(0);
  const [isPlaylistModalOpen, setIsPlaylistModalOpen] = useState(false);

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
        if (!mounted) return;

        setVideo(videoData || null);
        setSuggested(Array.isArray(feed) ? feed.filter((item) => item.id !== videoId) : []);

        // Record view silently
        recordVideoView(videoId).catch(() => {});

        if (user && videoData?.channelId) {
          try {
            const [isSub, resumeTime] = await Promise.all([
              checkSubscriptionStatus(videoData.channelId, user.id),
              getResumeTime(user.id, videoId)
            ]);
            if (mounted) {
              setSubscribed(!!isSub);
              if (resumeTime > 0) {
                setInitialResumeTime(resumeTime);
              }
            }
          } catch (e) {
            console.error("Failed to fetch sub status or resume time", e);
          }
        }
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
  }, [videoId, user]);

  const uploadedLabel = useMemo(() => {
    if (!video?.uploadedAt) {
      return "Recently";
    }
    return new Date(video.uploadedAt).toLocaleDateString();
  }, [video]);

  const handleSubscribeToggle = async () => {
    if (!user) {
      toast({ title: "Please log in to subscribe", variant: "destructive" });
      return;
    }
    if (!video?.channelId) return;
    
    setSubLoading(true);
    try {
      if (subscribed) {
        await unsubscribeFromChannel(video.channelId, user.id);
        setSubscribed(false);
        toast({ title: "Unsubscribed" });
      } else {
        await subscribeToChannel(video.channelId, user.id);
        setSubscribed(true);
        toast({ title: "Subscribed to channel" });
      }
    } catch (err) {
      toast({ title: "Action failed", description: err.message, variant: "destructive" });
    } finally {
      setSubLoading(false);
    }
  };

  const handleLikeToggle = async (isLike: boolean) => {
    if (!user) {
      toast({ title: "Please log in to vote", variant: "destructive" });
      return;
    }
    try {
      await toggleLikeVideo(videoId, user.id, isLike);
      if (isLike) {
        setLiked(!liked);
        setDisliked(false);
      } else {
        setDisliked(!disliked);
        setLiked(false);
      }
    } catch (err) {
      toast({ title: "Action failed", description: err.message, variant: "destructive" });
    }
  };

  const handleShare = async () => {
    const shareData = {
      title: video?.title || "StreamSphere video",
      url: window.location.href,
    };

    try {
      if (navigator.share) {
        await navigator.share(shareData);
      } else {
        await navigator.clipboard.writeText(shareData.url);
        toast({ title: "Link copied" });
      }
    } catch (err) {
      if (err?.name !== "AbortError") {
        toast({ title: "Unable to share", variant: "destructive" });
      }
    }
  };

  useEffect(() => {
    if (!user || !videoId || !videoRef.current) return;
    
    const interval = setInterval(() => {
      const currentTime = videoRef.current?.currentTime;
      if (currentTime && currentTime > 5 && !videoRef.current.paused) {
        updateResumeTime(user.id, videoId, currentTime).catch(() => {});
      }
    }, 10000);

    return () => clearInterval(interval);
  }, [user, videoId, videoRef]);

  const handleVideoLoaded = () => {
    if (videoRef.current && initialResumeTime > 0) {
      videoRef.current.currentTime = initialResumeTime;
      toast({ title: "Resumed from where you left off", duration: 3000 });
    }
  };

  return (
    <Layout>
      {loading && <p className="p-6 text-sm text-muted-foreground">Loading video...</p>}
      {!loading && error && <p className="p-6 text-sm text-red-500">{error}</p>}

      {!loading && !error && video && (
        <div className="flex flex-col gap-6 p-6 lg:flex-row">
          <div className="min-w-0 flex-1">
            <div className="aspect-video w-full overflow-hidden rounded-xl bg-black">
              <video 
                ref={videoRef}
                src={video.videoLink} 
                controls 
                className="h-full w-full"
                onLoadedMetadata={handleVideoLoaded}
              />
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
                {user && user.id !== video.channel?.userId && (
                  <Button 
                    variant={subscribed ? "secondary" : "default"} 
                    className="ml-4 rounded-full px-6"
                    onClick={handleSubscribeToggle}
                    disabled={subLoading}
                  >
                    {subscribed ? "Subscribed" : "Subscribe"}
                  </Button>
                )}
              </div>

              <div className="flex items-center gap-2">
                <div className="flex items-center overflow-hidden rounded-full bg-secondary">
                  <button
                    onClick={() => handleLikeToggle(true)}
                    className={`flex items-center gap-2 px-4 py-2 text-sm transition-colors hover:bg-surface-hover ${
                      liked ? "text-primary" : "text-foreground"
                    }`}
                    aria-pressed={liked}
                  >
                    <ThumbsUp className="h-4 w-4" />
                    Like
                  </button>
                  <div className="h-6 w-px bg-border" />
                  <button
                    onClick={() => handleLikeToggle(false)}
                    className={`px-4 py-2 hover:bg-surface-hover ${disliked ? "text-primary" : "text-foreground"}`}
                    aria-label="Dislike"
                    aria-pressed={disliked}
                  >
                    <ThumbsDown className="h-4 w-4" />
                  </button>
                </div>
                <Button variant="secondary" className="gap-2 rounded-full" onClick={handleShare}>
                  <Share2 className="h-4 w-4" /> Share
                </Button>
                <Button variant="secondary" className="gap-2 rounded-full" onClick={() => {
                  if (!user) {
                    toast({ title: "Please log in to save to playlist", variant: "destructive" });
                    return;
                  }
                  setIsPlaylistModalOpen(true);
                }}>
                  <ListPlus className="h-4 w-4" /> Save
                </Button>
                {video.videoLink && (
                  <Button asChild variant="secondary" className="gap-2 rounded-full">
                    <a href={video.videoLink} download>
                      <Download className="h-4 w-4" /> Download
                    </a>
                  </Button>
                )}
                <Button 
                  variant="ghost" 
                  className="gap-2 rounded-full text-red-400 hover:text-red-500 hover:bg-red-500/10" 
                  onClick={() => {
                    if (!user) {
                      toast({ title: "Please log in to report videos", variant: "destructive" });
                      return;
                    }
                    const reason = prompt("Reason for reporting this video:");
                    if (reason) {
                      import("@/lib/api").then(api => {
                        api.reportVideo(user.id, videoId, reason)
                          .then(() => toast({ title: "Video reported successfully" }))
                          .catch(() => toast({ title: "Failed to report video", variant: "destructive" }));
                      });
                    }
                  }}
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"></path><line x1="4" x2="4" y1="22" y2="15"></line></svg>
                  Report
                </Button>
              </div>
            </div>

            <div className="mt-4 rounded-xl bg-secondary p-3">
              <p className="text-sm font-medium text-foreground">
                {video.views || 0} views • {uploadedLabel}
              </p>
              <p className="mt-1 text-sm text-foreground/80">{video.description || "No description provided."}</p>
            </div>

            <CommentsSection videoId={videoId} />
          </div>

          <div className="w-full shrink-0 space-y-3 lg:w-96">
            <h3 className="text-sm font-medium text-foreground">Suggested</h3>
            {suggested.length === 0 && <p className="text-xs text-muted-foreground">No suggestions yet.</p>}
            {suggested.map((item) => (
              <Link key={item.id} to={`/watch/${item.id}`} className="group flex gap-2">
                <div className="relative aspect-video w-40 shrink-0 overflow-hidden rounded-lg bg-muted">
                  {item.thumbnailLink || item.thumbnailUrl ? (
                    <img
                      src={item.thumbnailLink || item.thumbnailUrl}
                      alt={item.title}
                      className="h-full w-full object-cover transition-transform group-hover:scale-105"
                      loading="lazy"
                    />
                  ) : (
                    <div className="flex h-full items-center justify-center">
                      <svg viewBox="0 0 24 24" className="h-8 w-8 fill-muted-foreground/30">
                        <path d="M10 8l6 4-6 4V8z" />
                      </svg>
                    </div>
                  )}
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
      
      {videoId && (
        <PlaylistModal
          videoId={videoId}
          channelId={String(video?.channelId ?? "")}
          isOpen={isPlaylistModalOpen}
          onClose={() => setIsPlaylistModalOpen(false)}
        />
      )}
    </Layout>
  );
};

export default WatchPage;
