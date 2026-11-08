import { Layout } from "@/components/Layout";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import CommentsSection from "@/components/CommentsSection";
import { useAuth } from "@/contexts/AuthContext";
import { checkSubscriptionStatus, subscribeToChannel, unsubscribeFromChannel, toggleLikeVideo, recordVideoView, getVideoById, getVideoFeed, getMyChannel, getResumeTime, updateResumeTime, isLegacyLocalMediaUrl } from "@/lib/api";
import { toast } from "@/hooks/use-toast";
import {
  ArrowUpDown,
  Download,
  Flag,
  ListPlus,
  Pause,
  Play,
  Repeat2,
  Share2,
  SkipBack,
  SkipForward,
  ThumbsDown,
  ThumbsUp,
  X,
} from "lucide-react";
import { useEffect, useMemo, useState, useRef } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { PlaylistModal } from "@/components/PlaylistModal";
import { ReportVideoDialog } from "@/components/ReportVideoDialog";
import { Input } from "@/components/ui/input";

const WatchPage = () => {
  const { videoId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const [video, setVideo] = useState(null);
  const [suggested, setSuggested] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [playbackError, setPlaybackError] = useState(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isChannelOwner, setIsChannelOwner] = useState(false);
  const [liked, setLiked] = useState(false);
  const [disliked, setDisliked] = useState(false);
  const [subscribed, setSubscribed] = useState(false);
  const [subLoading, setSubLoading] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [initialResumeTime, setInitialResumeTime] = useState(0);
  const [isPlaylistModalOpen, setIsPlaylistModalOpen] = useState(false);
  const [isReportDialogOpen, setIsReportDialogOpen] = useState(false);
  const [queueOpen, setQueueOpen] = useState(false);
  const [queueCollapsed, setQueueCollapsed] = useState(false);
  const [queueShuffled, setQueueShuffled] = useState(false);
  const [repeatCurrentVideo, setRepeatCurrentVideo] = useState(false);
  const [queueOrder, setQueueOrder] = useState<string[]>([]);
  const [playerHeight, setPlayerHeight] = useState(0);
  const [viewportHeight, setViewportHeight] = useState(
    typeof window === "undefined" ? 800 : window.innerHeight,
  );
  const playerSectionRef = useRef<HTMLDivElement>(null);

  const uploadedLabel = useMemo(() => formatUploadDate(video?.uploadedAt), [video?.uploadedAt]);

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
      setPlaybackError(false);
      setIsPlaying(false);
      setLiked(false);
      setDisliked(false);
      setSubscribed(false);
      setIsChannelOwner(false);
      setQueueOpen(false);
      setQueueCollapsed(false);
      setInitialResumeTime(0);

      try {
        const [videoData, feed] = await Promise.all([getVideoById(videoId), getVideoFeed(20)]);
        if (!mounted) return;

        setVideo(videoData || null);
        setSuggested(Array.isArray(feed) ? feed : []);

        if (user?.email && videoData?.channelId) {
          try {
            const channel = await getMyChannel(user.email);
            if (mounted) {
              setIsChannelOwner(channel?.id === videoData.channelId);
            }
          } catch (channelError) {
            console.error("Failed to determine channel ownership", channelError);
          }
        } else if (mounted) {
          setIsChannelOwner(false);
        }

        // Record view silently
        recordVideoView(videoId).catch(() => {});

        if (user?.id && videoData?.channelId) {
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

  useEffect(() => {
    const player = playerSectionRef.current;
    if (!player) return;

    const updateMeasurements = () => {
      setPlayerHeight(player.getBoundingClientRect().height);
      setViewportHeight(window.innerHeight);
    };
    updateMeasurements();
    window.addEventListener("resize", updateMeasurements);
    const observer = typeof ResizeObserver === "undefined"
      ? null
      : new ResizeObserver(updateMeasurements);
    observer?.observe(player);
    return () => {
      window.removeEventListener("resize", updateMeasurements);
      observer?.disconnect();
    };
  }, [video?.id]);

  const queueSuggestions = useMemo(() => {
    const candidates = suggested.filter((item) => item.id !== video?.id);
    if (!queueShuffled || !queueOrder.length) return candidates;
    const orderById = new Map(queueOrder.map((id, index) => [id, index]));
    return [...candidates].sort(
      (left, right) =>
        (orderById.get(left.id) ?? Number.MAX_SAFE_INTEGER)
        - (orderById.get(right.id) ?? Number.MAX_SAFE_INTEGER),
    );
  }, [queueOrder, queueShuffled, suggested, video?.id]);

  const queueItems = useMemo(
    () => {
      if (!video) return suggested;
      const currentIndex = suggested.findIndex((item) => item.id === video.id);
      const candidates = currentIndex < 0
        ? [video, ...suggested]
        : suggested.map((item) => item.id === video.id ? video : item);
      if (!queueShuffled || !queueOrder.length) return candidates;
      const orderById = new Map(queueOrder.map((id, index) => [id, index]));
      return [...candidates].sort(
        (left, right) =>
          (orderById.get(left.id) ?? Number.MAX_SAFE_INTEGER)
          - (orderById.get(right.id) ?? Number.MAX_SAFE_INTEGER),
      );
    },
    [queueOrder, queueShuffled, suggested, video],
  );
  const currentQueueIndex = queueItems.findIndex((item) => item.id === video?.id);
  const previousQueueItem = currentQueueIndex > 0 ? queueItems[currentQueueIndex - 1] : null;
  const nextQueueItem = currentQueueIndex >= 0 ? queueItems[currentQueueIndex + 1] : null;

  const navigateInQueue = (direction: "previous" | "next") => {
    const target = direction === "previous" ? previousQueueItem : nextQueueItem;
    if (target) navigate(`/watch/${target.id}`);
  };

  const togglePlayback = () => {
    const player = videoRef.current;
    if (!player) return;
    if (player.paused) {
      void player.play().catch(() => {
        toast({ title: "Unable to play video", description: "Use the player controls to try again.", variant: "destructive" });
      });
    } else {
      player.pause();
    }
  };

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
    if (!user?.id || !videoId || video?.id !== videoId || !videoRef.current) return;
    
    const interval = setInterval(() => {
      const currentTime = videoRef.current?.currentTime;
      if (currentTime && currentTime > 5 && !videoRef.current.paused) {
        updateResumeTime(user.id, videoId, currentTime).catch(() => {});
      }
    }, 10000);

    return () => clearInterval(interval);
  }, [user, videoId, video?.id]);

  const handleVideoLoaded = () => {
    if (videoRef.current && initialResumeTime > 0) {
      videoRef.current.currentTime = initialResumeTime;
      toast({ title: "Resumed from where you left off", duration: 3000 });
    }
  };

  const handleVideoEnded = () => {
    if (repeatCurrentVideo && videoRef.current) {
      videoRef.current.currentTime = 0;
      void videoRef.current.play().catch(() => {});
      return;
    }
    if (nextQueueItem?.id) navigate(`/watch/${nextQueueItem.id}`);
  };

  const toggleQueueShuffle = () => {
    if (queueShuffled) {
      setQueueShuffled(false);
      setQueueOrder([]);
      return;
    }
    const ids = queueItems.map((item) => item.id);
    for (let index = ids.length - 1; index > 0; index--) {
      const swapIndex = Math.floor(Math.random() * (index + 1));
      [ids[index], ids[swapIndex]] = [ids[swapIndex], ids[index]];
    }
    setQueueOrder(ids);
    setQueueShuffled(true);
  };

  return (
    <Layout>
      {loading && !video && <p className="p-6 text-sm text-muted-foreground">Loading video...</p>}
      {!loading && error && <p className="p-6 text-sm text-red-500">{error}</p>}
      {loading && video && <p className="px-3 pt-2 text-xs text-muted-foreground sm:px-6">Loading next video…</p>}

      {!error && video && (
        <div className="flex w-full min-w-0 flex-col gap-4 px-3 py-4 sm:gap-6 sm:p-6 lg:flex-row">
          <div className="w-full min-w-0 flex-1">
            <div
              ref={playerSectionRef}
              className="sticky top-14 z-40 -mx-3 w-auto bg-background pb-2 sm:mx-0 sm:pb-0"
            >
              <div className="group relative aspect-video w-full max-w-full overflow-hidden bg-black sm:rounded-xl">
                <video
                  ref={videoRef}
                  src={video.videoLink}
                  controls
                  autoPlay
                  playsInline
                  preload="metadata"
                  className="absolute inset-0 h-full w-full object-contain"
                  onLoadedMetadata={handleVideoLoaded}
                  onEnded={handleVideoEnded}
                  onPlay={() => setIsPlaying(true)}
                  onPause={() => setIsPlaying(false)}
                  onError={() => setPlaybackError(true)}
                  onLoadStart={() => setPlaybackError(false)}
                />
                <div
                  role="group"
                  className="pointer-events-none absolute inset-0 z-10 flex items-center justify-center"
                  aria-label="Video navigation controls"
                >
                  <div className="pointer-events-auto flex items-center gap-2 rounded-full bg-black/55 p-2 text-white shadow-lg backdrop-blur-sm sm:gap-4 sm:p-3">
                    <button
                      type="button"
                      aria-label="Play previous video"
                      title={previousQueueItem ? "Previous video" : "No previous video"}
                      disabled={!previousQueueItem}
                      onClick={() => navigateInQueue("previous")}
                      className="flex h-10 w-10 items-center justify-center rounded-full transition hover:bg-white/15 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white disabled:cursor-not-allowed disabled:opacity-40 sm:h-12 sm:w-12"
                    >
                      <SkipBack className="h-6 w-6" />
                    </button>
                    <button
                      type="button"
                      aria-label={isPlaying ? "Pause video" : "Play video"}
                      onClick={togglePlayback}
                      className="flex h-12 w-12 items-center justify-center rounded-full bg-white/15 transition hover:bg-white/25 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white sm:h-14 sm:w-14"
                    >
                      {isPlaying ? <Pause className="h-6 w-6 fill-current" /> : <Play className="ml-1 h-6 w-6 fill-current" />}
                    </button>
                    <button
                      type="button"
                      aria-label="Play next video"
                      title={nextQueueItem ? "Next video" : "No next video"}
                      disabled={!nextQueueItem}
                      onClick={() => navigateInQueue("next")}
                      className="flex h-10 w-10 items-center justify-center rounded-full transition hover:bg-white/15 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-white disabled:cursor-not-allowed disabled:opacity-40 sm:h-12 sm:w-12"
                    >
                      <SkipForward className="h-6 w-6" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
            {playbackError && (
              <p className="mt-2 text-sm text-red-500" role="alert">
                {isLegacyLocalMediaUrl(video.videoLink)
                  ? "This older video points to temporary server storage and its original file is missing. The channel owner must upload the original file again."
                  : "This video could not be played. The media may be unavailable or still processing."}
              </p>
            )}
            {isLegacyLocalMediaUrl(video.videoLink) && isChannelOwner && (
              <Button asChild className="mt-3 rounded-full">
                <Link to={`/upload?replaceVideoId=${encodeURIComponent(video.id)}`}>
                  Replace missing video file
                </Link>
              </Button>
            )}

            <h1 className="mt-4 break-words text-lg font-semibold text-foreground sm:text-xl">{video.title || "Untitled video"}</h1>

            <div className="mt-3 flex min-w-0 flex-col items-start gap-3 sm:flex-row sm:flex-wrap sm:items-center sm:justify-between sm:gap-4">
              <div className="flex min-w-0 flex-wrap items-center gap-3">
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
                    className="rounded-full px-4 sm:ml-1 sm:px-6"
                    onClick={handleSubscribeToggle}
                    disabled={subLoading}
                  >
                    {subscribed ? "Subscribed" : "Subscribe"}
                  </Button>
                )}
              </div>

              <div className="flex w-full min-w-0 flex-wrap items-center gap-2 sm:w-auto">
                <div className="flex items-center overflow-hidden rounded-full bg-secondary">
                  <button
                    onClick={() => handleLikeToggle(true)}
                    className={`flex items-center gap-2 px-3 py-2 text-sm transition-colors hover:bg-surface-hover sm:px-4 ${
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
                <Button variant="secondary" className="gap-2 rounded-full px-3 sm:px-4" onClick={handleShare}>
                  <Share2 className="h-4 w-4" /> Share
                </Button>
                <Button
                  variant="secondary"
                  className="gap-2 rounded-full px-3 sm:px-4"
                  onClick={() => {
                    if (!user) {
                      toast({ title: "Please log in to save to playlist", variant: "destructive" });
                      return;
                    }
                    setIsPlaylistModalOpen(true);
                  }}
                >
                  <ListPlus className="h-4 w-4" /> Save
                </Button>
                {video.videoLink && (
                  <Button asChild variant="secondary" className="gap-2 rounded-full px-3 sm:px-4">
                    <a href={video.videoLink} download>
                      <Download className="h-4 w-4" /> Download
                    </a>
                  </Button>
                )}
                <Button
                  variant="ghost"
                  className="shrink-0 gap-2 rounded-full px-3 text-red-400 hover:bg-red-500/10 hover:text-red-500 sm:px-4"
                  onClick={() => {
                    if (!user) {
                      toast({ title: "Please log in to report videos", variant: "destructive" });
                      return;
                    }
                    setIsReportDialogOpen(true);
                  }}
                >
                  <Flag className="h-4 w-4" />
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

          <div className="w-full min-w-0 shrink-0 space-y-3 lg:w-96">
            <h3 className="text-sm font-medium text-foreground">Suggested</h3>
            <Button
              type="button"
              size="sm"
              variant="secondary"
              className="mb-1 w-full justify-between rounded-lg"
              onClick={() => {
                setQueueCollapsed(false);
                setQueueOpen(true);
              }}
            >
              <span>Open playback queue</span>
              <span className="text-xs text-muted-foreground">{queueItems.length} videos</span>
            </Button>
            {queueSuggestions.length === 0 && <p className="text-xs text-muted-foreground">No suggestions yet.</p>}
            {queueSuggestions.map((item) => (
              <Link key={item.id} to={`/watch/${item.id}`} className="group flex gap-2">
                <div className="relative aspect-video w-40 shrink-0 overflow-hidden rounded-lg bg-muted">
                  <img
                    src={item.thumbnailLink || item.thumbnailUrl || "/thumbnail-placeholder.svg"}
                    alt={item.title ? `Thumbnail for ${item.title}` : "Video thumbnail"}
                    className="h-full w-full object-cover transition-transform group-hover:scale-105"
                    loading="lazy"
                    decoding="async"
                    onError={(event) => {
                      if (!event.currentTarget.src.endsWith("/thumbnail-placeholder.svg")) {
                        event.currentTarget.src = "/thumbnail-placeholder.svg";
                      }
                    }}
                  />
                  {formatVideoDuration(item.durationSeconds ?? item.duration) && (
                    <span className="absolute bottom-1 right-1 rounded bg-black/80 px-1.5 py-0.5 text-[10px] text-white">
                      {formatVideoDuration(item.durationSeconds ?? item.duration)}
                    </span>
                  )}
                </div>
                <div className="min-w-0">
                  <p className="line-clamp-2 text-sm font-medium text-foreground">{item.title}</p>
                  <p className="mt-1 text-xs text-muted-foreground">{item.channelName}</p>
                  <p className="text-xs text-muted-foreground">
                    {item.views || 0} views · {formatUploadDate(item.uploadedAt)}
                  </p>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}

      {video && (
        <aside
          className="fixed inset-x-0 bottom-0 z-[45] px-2 pb-[max(env(safe-area-inset-bottom),0.5rem)] sm:px-4"
          aria-label="Playback queue"
        >
          {!queueOpen ? (
            <Button
              type="button"
              className="mx-auto flex min-h-12 w-full max-w-3xl justify-between rounded-t-xl rounded-b-md shadow-lg"
              onClick={() => {
                setQueueCollapsed(false);
                setQueueOpen(true);
              }}
              aria-label={`Open playback queue with ${queueItems.length} videos`}
            >
              <span className="truncate">Up next · {video.title || "Now playing"}</span>
              <span className="shrink-0 text-xs opacity-80">{queueItems.length} videos</span>
            </Button>
          ) : (
            <section
              role="region"
              aria-label="Up next videos"
              className="mx-auto flex w-full max-w-5xl flex-col overflow-hidden rounded-t-2xl border border-border bg-background text-foreground shadow-2xl"
              style={{
                maxHeight: `${Math.max(80, Math.min(520, viewportHeight - 56 - playerHeight - 16))}px`,
              }}
            >
              <button
                type="button"
                aria-label={queueCollapsed ? "Expand playback queue" : "Collapse playback queue"}
                aria-expanded={!queueCollapsed}
                onClick={() => setQueueCollapsed((collapsed) => !collapsed)}
                className="flex h-7 shrink-0 items-center justify-center"
              >
                <span className="h-1 w-10 rounded-full bg-muted-foreground/40" />
              </button>
              <div className="flex shrink-0 items-center gap-3 border-b px-4 pb-3">
                <div className="min-w-0 flex-1">
                  <h2 className="truncate text-sm font-semibold">Up next</h2>
                  <p className="truncate text-xs text-muted-foreground">
                    {video.channelName || "Recommended for you"} · {currentQueueIndex + 1} / {queueItems.length}
                  </p>
                </div>
                <Button
                  type="button"
                  variant={queueShuffled ? "secondary" : "ghost"}
                  size="icon"
                  aria-label={queueShuffled ? "Turn shuffle off" : "Shuffle queue"}
                  aria-pressed={queueShuffled}
                  onClick={toggleQueueShuffle}
                >
                  <ArrowUpDown className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  variant={repeatCurrentVideo ? "secondary" : "ghost"}
                  size="icon"
                  aria-label={repeatCurrentVideo ? "Turn repeat off" : "Repeat current video"}
                  aria-pressed={repeatCurrentVideo}
                  onClick={() => setRepeatCurrentVideo((repeat) => !repeat)}
                >
                  <Repeat2 className="h-4 w-4" />
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  aria-label="Close playback queue"
                  onClick={() => setQueueOpen(false)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              {!queueCollapsed && (
                <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain p-2">
                  {queueItems.map((item, index) => {
                    const isCurrent = item.id === video.id;
                    return (
                      <Link
                        key={item.id}
                        to={`/watch/${item.id}`}
                        onClick={() => setQueueOpen(false)}
                        aria-current={isCurrent ? "page" : undefined}
                        className={`flex min-h-[76px] items-center gap-3 rounded-lg p-2 transition-colors ${
                          isCurrent ? "bg-accent" : "hover:bg-accent/70"
                        }`}
                      >
                        <span className="w-5 shrink-0 text-center text-xs text-muted-foreground">
                          {isCurrent ? "▶" : index + 1}
                        </span>
                        <div className="relative aspect-video w-28 shrink-0 overflow-hidden rounded-md bg-muted sm:w-36">
                          <img
                            src={item.thumbnailLink || item.thumbnailUrl || "/thumbnail-placeholder.svg"}
                            alt=""
                            className="h-full w-full object-cover"
                            loading="lazy"
                            onError={(event) => {
                              if (!event.currentTarget.src.endsWith("/thumbnail-placeholder.svg")) {
                                event.currentTarget.src = "/thumbnail-placeholder.svg";
                              }
                            }}
                          />
                          {formatVideoDuration(item.durationSeconds ?? item.duration) && (
                            <span className="absolute bottom-1 right-1 rounded bg-black/80 px-1 py-0.5 text-[10px] text-white">
                              {formatVideoDuration(item.durationSeconds ?? item.duration)}
                            </span>
                          )}
                        </div>
                        <div className="min-w-0 flex-1">
                          <p className="line-clamp-2 text-sm font-medium">{item.title || "Untitled video"}</p>
                          <p className="mt-1 truncate text-xs text-muted-foreground">
                            {item.channelName || "Unknown channel"} · {item.views || 0} views · {formatUploadDate(item.uploadedAt)}
                          </p>
                        </div>
                      </Link>
                    );
                  })}
                  {!queueItems.length && (
                    <p className="p-4 text-sm text-muted-foreground">No videos are available in this queue.</p>
                  )}
                </div>
              )}
            </section>
          )}
        </aside>
      )}
      
      {videoId && (
        <PlaylistModal
          videoId={videoId}
          channelId={String(video?.channelId ?? "")}
          isOpen={isPlaylistModalOpen}
          onClose={() => setIsPlaylistModalOpen(false)}
        />
      )}
      {videoId && (
        <ReportVideoDialog
          videoId={videoId}
          open={isReportDialogOpen}
          onOpenChange={setIsReportDialogOpen}
        />
      )}
    </Layout>
  );
};

export default WatchPage;

function formatUploadDate(value?: string | null) {
  if (!value) return "Recently";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Recently" : date.toLocaleDateString();
}

function formatVideoDuration(value?: number | string | null) {
  if (typeof value === "string" && value.trim()) return value;
  const durationSeconds = Number(value);
  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) return "";
  const seconds = Math.floor(durationSeconds);
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainder = seconds % 60;
  return hours > 0
    ? `${hours}:${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`
    : `${minutes}:${String(remainder).padStart(2, "0")}`;
}
