import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowLeft,
  Check,
  Ellipsis,
  Flag,
  Heart,
  MessageCircle,
  MoreHorizontal,
  Pause,
  Play,
  Search,
  Share2,
  Volume2,
  VolumeX,
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { toast } from "@/hooks/use-toast";
import {
  checkSubscriptionStatus,
  getShorts,
  getVideoEngagement,
  subscribeToChannel,
  toggleLikeVideo,
  unsubscribeFromChannel,
} from "@/lib/api";
import { ReportVideoDialog } from "@/components/ReportVideoDialog";
import CommentsSection from "@/components/CommentsSection";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
} from "@/components/ui/sheet";

interface ShortVideo {
  id: string;
  title: string;
  description?: string;
  videoLink?: string;
  thumbnailLink?: string;
  views?: number;
  channelName?: string;
  channelId?: string;
}

interface EngagementSummary {
  likeCount: number;
  commentCount: number;
  likedByCurrentUser: boolean;
}

const EMPTY_ENGAGEMENT: EngagementSummary = {
  likeCount: 0,
  commentCount: 0,
  likedByCurrentUser: false,
};

const ShortsPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [shorts, setShorts] = useState<ShortVideo[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [muted, setMuted] = useState(true);
  const [paused, setPaused] = useState(false);
  const [bufferingId, setBufferingId] = useState<string | null>(null);
  const [playbackErrorId, setPlaybackErrorId] = useState<string | null>(null);
  const [autoplayBlockedId, setAutoplayBlockedId] = useState<string | null>(null);
  const [engagement, setEngagement] = useState<EngagementSummary>(EMPTY_ENGAGEMENT);
  const [engagementLoading, setEngagementLoading] = useState(false);
  const [likePending, setLikePending] = useState(false);
  const [subscribed, setSubscribed] = useState(false);
  const [subscribePending, setSubscribePending] = useState(false);
  const [descriptionExpanded, setDescriptionExpanded] = useState(false);
  const [commentsOpen, setCommentsOpen] = useState(false);
  const [reportOpen, setReportOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const feedRef = useRef<HTMLDivElement>(null);
  const videoRefs = useRef<Record<string, HTMLVideoElement | null>>({});
  const observerRef = useRef<IntersectionObserver | null>(null);
  const engagementRequestRef = useRef(0);

  const activeIndex = useMemo(
    () => shorts.findIndex((short) => short.id === activeId),
    [activeId, shorts],
  );
  const activeShort = activeIndex >= 0 ? shorts[activeIndex] : null;

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    getShorts(30)
      .then((feed) => {
        if (!active) return;
        const validShorts = Array.isArray(feed)
          ? feed.filter((short) => typeof short.id === "string" && Boolean(short.videoLink))
          : [];
        setShorts(validShorts);
        setActiveId(validShorts[0]?.id ?? null);
      })
      .catch((loadError) => {
        if (active) setError(loadError instanceof Error ? loadError.message : "Could not load Shorts.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });
    return () => { active = false; };
  }, []);

  useEffect(() => {
    const root = feedRef.current;
    if (!root || !shorts.length) return;

    observerRef.current?.disconnect();
    const observer = new IntersectionObserver(
      (entries) => {
        const mostVisible = entries
          .filter((entry) => entry.isIntersecting && entry.intersectionRatio >= 0.6)
          .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
        const shortId = mostVisible?.target.getAttribute("data-short-id");
        if (shortId) setActiveId(shortId);
      },
      { root, threshold: [0.6, 0.75, 0.9] },
    );
    observerRef.current = observer;
    root.querySelectorAll<HTMLElement>("[data-short-id]").forEach((item) => observer.observe(item));
    return () => {
      observer.disconnect();
      if (observerRef.current === observer) observerRef.current = null;
    };
  }, [shorts]);

  useEffect(() => {
    for (const [id, player] of Object.entries(videoRefs.current)) {
      if (!player) continue;
      player.muted = muted;
      if (id !== activeId) player.pause();
    }

    if (!activeId) return;
    const player = videoRefs.current[activeId];
    if (!player) return;
    setPaused(false);
    setBufferingId(activeId);
    setPlaybackErrorId(null);
    void player.play()
      .then(() => setAutoplayBlockedId((blockedId) => blockedId === activeId ? null : blockedId))
      .catch((playError: DOMException) => {
        if (playError.name === "NotAllowedError") {
          setAutoplayBlockedId(activeId);
        } else if (playError.name !== "AbortError") {
          setPlaybackErrorId(activeId);
        }
      });
  }, [activeId, muted]);

  const refreshEngagement = useCallback(async (videoId: string) => {
    const requestId = ++engagementRequestRef.current;
    setEngagementLoading(true);
    try {
      const result = await getVideoEngagement(videoId);
      if (requestId === engagementRequestRef.current) {
        setEngagement({
          likeCount: Number(result.likeCount) || 0,
          commentCount: Number(result.commentCount) || 0,
          likedByCurrentUser: Boolean(result.likedByCurrentUser),
        });
      }
    } catch (requestError) {
      if (requestId === engagementRequestRef.current) {
        toast({
          title: "Engagement details unavailable",
          description: requestError instanceof Error ? requestError.message : undefined,
          variant: "destructive",
        });
      }
    } finally {
      if (requestId === engagementRequestRef.current) setEngagementLoading(false);
    }
  }, []);

  useEffect(() => {
    setDescriptionExpanded(false);
    setMenuOpen(false);
    setEngagement(EMPTY_ENGAGEMENT);
    setSubscribed(false);
    if (!activeShort) return;
    void refreshEngagement(activeShort.id);
    if (user?.id && activeShort.channelId) {
      checkSubscriptionStatus(activeShort.channelId, user.id)
        .then((result) => {
          if (activeId === activeShort.id) setSubscribed(Boolean(result));
        })
        .catch(() => setSubscribed(false));
    }
  }, [activeId, activeShort, refreshEngagement, user?.id]);

  useEffect(() => {
    const root = feedRef.current;
    if (!root) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.target instanceof HTMLElement && /^(INPUT|TEXTAREA|BUTTON|SELECT)$/.test(event.target.tagName)) return;
      if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
      event.preventDefault();
      const nextIndex = Math.max(0, Math.min(shorts.length - 1, activeIndex + (event.key === "ArrowDown" ? 1 : -1)));
      const nextShort = shorts[nextIndex];
      if (nextShort) {
        root.querySelector<HTMLElement>(`[data-short-id="${CSS.escape(nextShort.id)}"]`)
          ?.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }
    root.addEventListener("keydown", onKeyDown);
    return () => root.removeEventListener("keydown", onKeyDown);
  }, [activeIndex, shorts]);

  async function togglePlayback(short: ShortVideo) {
    const player = videoRefs.current[short.id];
    if (!player) return;
    if (player.paused) {
      try {
        await player.play();
        setPaused(false);
        setAutoplayBlockedId(null);
      } catch {
        setAutoplayBlockedId(short.id);
      }
    } else {
      player.pause();
      setPaused(true);
    }
  }

  async function handleLike() {
    if (!activeShort) return;
    if (!user?.id) {
      toast({ title: "Sign in to like videos" });
      return;
    }
    if (likePending) return;
    setLikePending(true);
    try {
      await toggleLikeVideo(activeShort.id, user.id, true);
      await refreshEngagement(activeShort.id);
    } catch (likeError) {
      toast({
        title: "Could not update your like",
        description: likeError instanceof Error ? likeError.message : undefined,
        variant: "destructive",
      });
    } finally {
      setLikePending(false);
    }
  }

  async function handleSubscribe() {
    if (!activeShort?.channelId) return;
    if (!user?.id) {
      toast({ title: "Sign in to subscribe" });
      return;
    }
    if (subscribePending) return;
    setSubscribePending(true);
    try {
      if (subscribed) {
        await unsubscribeFromChannel(activeShort.channelId, user.id);
        setSubscribed(false);
      } else {
        await subscribeToChannel(activeShort.channelId, user.id);
        setSubscribed(true);
      }
    } catch (subscriptionError) {
      toast({
        title: "Could not update subscription",
        description: subscriptionError instanceof Error ? subscriptionError.message : undefined,
        variant: "destructive",
      });
    } finally {
      setSubscribePending(false);
    }
  }

  async function handleShare() {
    if (!activeShort) return;
    const url = new URL(`/watch/${activeShort.id}`, window.location.origin).toString();
    try {
      if (navigator.share) {
        await navigator.share({ title: activeShort.title, url });
      } else {
        await navigator.clipboard.writeText(url);
        toast({ title: "Link copied" });
      }
    } catch (shareError) {
      if (!(shareError instanceof DOMException && shareError.name === "AbortError")) {
        toast({ title: "Unable to share this Short", variant: "destructive" });
      }
    }
  }

  if (loading) {
    return <main className="fixed inset-0 z-40 grid place-items-center bg-black text-sm text-white">Loading Shorts…</main>;
  }

  if (error || shorts.length === 0) {
    return (
      <main className="fixed inset-0 z-40 flex flex-col items-center justify-center gap-4 bg-black px-6 text-center text-white">
        <p role={error ? "alert" : "status"} className="text-sm text-white/75">{error || "No Shorts are available right now."}</p>
        <Button variant="secondary" onClick={() => navigate("/")}>Back to StreamSphere</Button>
      </main>
    );
  }

  return (
    <main className="fixed inset-0 z-40 overflow-hidden bg-black text-white">
      <div
        ref={feedRef}
        tabIndex={0}
        aria-label="Shorts feed. Use the up and down arrow keys to navigate."
        className="h-full w-full snap-y snap-mandatory overflow-y-auto overscroll-y-contain outline-none [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {shorts.map((short, index) => {
          const isActive = short.id === activeId;
          const needsSource = Math.abs(index - activeIndex) <= 1;
          return (
            <section
              key={short.id}
              id={`short-${short.id}`}
              data-short-id={short.id}
              aria-label={`Short ${index + 1} of ${shorts.length}: ${short.title}`}
              className="relative h-screen h-[100dvh] w-full snap-start snap-always overflow-hidden bg-black"
            >
              <video
                ref={(element) => { videoRefs.current[short.id] = element; }}
                src={needsSource ? short.videoLink : undefined}
                poster={short.thumbnailLink || undefined}
                muted={muted}
                playsInline
                loop
                preload={isActive ? "auto" : index === activeIndex + 1 ? "metadata" : "none"}
                className="absolute inset-0 h-full w-full object-cover"
                onClick={() => isActive && void togglePlayback(short)}
                onWaiting={() => isActive && setBufferingId(short.id)}
                onCanPlay={() => setBufferingId((current) => current === short.id ? null : current)}
                onPlaying={() => {
                  setBufferingId((current) => current === short.id ? null : current);
                  if (isActive) {
                    setPaused(false);
                    setPlaybackErrorId(null);
                    setAutoplayBlockedId(null);
                  }
                }}
                onError={() => isActive && setPlaybackErrorId(short.id)}
              />
              <div className="pointer-events-none absolute inset-0 bg-gradient-to-b from-black/25 via-transparent to-black/75" />

              {isActive && (
                <>
                  <header className="absolute inset-x-0 top-0 z-20 flex items-center justify-between px-3 pt-[max(env(safe-area-inset-top),0.75rem)] sm:px-5">
                    <button
                      type="button"
                      aria-label="Back to home"
                      onClick={() => navigate("/")}
                      className="flex h-11 w-11 items-center justify-center rounded-full bg-black/35 text-white backdrop-blur-md"
                    >
                      <ArrowLeft className="h-5 w-5" />
                    </button>
                    <div className="flex items-center gap-2">
                      <Link
                        to="/search"
                        aria-label="Search videos"
                        className="flex h-11 w-11 items-center justify-center rounded-full bg-black/35 text-white backdrop-blur-md"
                      >
                        <Search className="h-5 w-5" />
                      </Link>
                      <button
                        type="button"
                        aria-label="More video actions"
                        aria-expanded={menuOpen}
                        onClick={() => setMenuOpen((open) => !open)}
                        className="flex h-11 w-11 items-center justify-center rounded-full bg-black/35 text-white backdrop-blur-md"
                      >
                        <Ellipsis className="h-5 w-5" />
                      </button>
                      <button
                        type="button"
                        aria-label={muted ? "Unmute Shorts" : "Mute Shorts"}
                        onClick={() => setMuted((value) => !value)}
                        className="flex h-11 w-11 items-center justify-center rounded-full bg-black/35 text-white backdrop-blur-md"
                      >
                        {muted ? <VolumeX className="h-5 w-5" /> : <Volume2 className="h-5 w-5" />}
                      </button>
                    </div>
                  </header>

                  {menuOpen && (
                    <div className="absolute right-3 top-16 z-30 min-w-48 rounded-xl border border-white/10 bg-zinc-950/95 p-1 shadow-xl backdrop-blur-md">
                      <button
                        type="button"
                        onClick={() => { setMenuOpen(false); void handleShare(); }}
                        className="flex min-h-11 w-full items-center gap-3 rounded-lg px-3 text-left text-sm hover:bg-white/10"
                      ><Share2 className="h-4 w-4" /> Share</button>
                      <button
                        type="button"
                        onClick={() => {
                          setMenuOpen(false);
                          if (!user) toast({ title: "Sign in to report videos" });
                          else setReportOpen(true);
                        }}
                        className="flex min-h-11 w-full items-center gap-3 rounded-lg px-3 text-left text-sm hover:bg-white/10"
                      ><Flag className="h-4 w-4" /> Report</button>
                      <Link
                        to={`/watch/${short.id}`}
                        className="flex min-h-11 items-center gap-3 rounded-lg px-3 text-sm hover:bg-white/10"
                      ><Play className="h-4 w-4" /> Open video page</Link>
                    </div>
                  )}

                  <aside className="absolute bottom-[calc(9.5rem+env(safe-area-inset-bottom))] right-2 z-20 flex flex-col items-center gap-4 sm:right-5 sm:gap-6">
                    <button
                      type="button"
                      aria-label={engagement.likedByCurrentUser ? "Unlike video" : "Like video"}
                      aria-pressed={engagement.likedByCurrentUser}
                      disabled={likePending || engagementLoading}
                      onClick={() => void handleLike()}
                      className="flex min-h-14 min-w-12 flex-col items-center justify-center gap-1 text-shadow disabled:opacity-70"
                    >
                      <Heart className={`h-7 w-7 drop-shadow ${engagement.likedByCurrentUser ? "fill-rose-500 text-rose-500" : "text-white"}`} />
                      <span className="max-w-14 truncate text-xs font-medium drop-shadow">{engagement.likeCount.toLocaleString()}</span>
                    </button>
                    <button
                      type="button"
                      aria-label={`Open ${engagement.commentCount} comments`}
                      onClick={() => setCommentsOpen(true)}
                      className="flex min-h-14 min-w-12 flex-col items-center justify-center gap-1"
                    >
                      <MessageCircle className="h-7 w-7 drop-shadow" />
                      <span className="max-w-14 truncate text-xs font-medium drop-shadow">{engagement.commentCount.toLocaleString()}</span>
                    </button>
                    <button
                      type="button"
                      aria-label="Share Short"
                      onClick={() => void handleShare()}
                      className="flex min-h-14 min-w-12 flex-col items-center justify-center gap-1"
                    >
                      <Share2 className="h-7 w-7 drop-shadow" />
                      <span className="text-xs font-medium drop-shadow">Share</span>
                    </button>
                  </aside>

                  <div className="absolute inset-x-0 bottom-0 z-10 pb-[max(env(safe-area-inset-bottom),1rem)] pl-4 pr-[4.75rem] pt-16 text-white sm:pl-6 sm:pr-24">
                    <div className="mb-3 flex min-w-0 items-center gap-2.5">
                      <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full border border-white/30 bg-white/15 font-semibold">
                        {(short.channelName || "C").slice(0, 1).toUpperCase()}
                      </div>
                      <span className="min-w-0 truncate text-sm font-semibold drop-shadow">{short.channelName || "StreamSphere creator"}</span>
                      {short.channelId && (
                        <Button
                          type="button"
                          size="sm"
                          variant={subscribed ? "secondary" : "default"}
                          disabled={subscribePending}
                          onClick={() => void handleSubscribe()}
                          className="min-h-9 shrink-0 rounded-full px-4"
                        >
                          {subscribed ? <><Check className="mr-1 h-4 w-4" /> Subscribed</> : "Subscribe"}
                        </Button>
                      )}
                    </div>
                    <h1 className="mb-1 line-clamp-1 text-sm font-semibold drop-shadow sm:text-base">{short.title}</h1>
                    {short.description && (
                      <div className="max-w-2xl">
                        <p className={`${descriptionExpanded ? "max-h-28 overflow-y-auto" : "line-clamp-2"} whitespace-pre-line break-words text-xs leading-relaxed text-white/90 drop-shadow sm:text-sm`}>
                          {short.description}
                        </p>
                        {short.description.length > 100 && (
                          <button
                            type="button"
                            onClick={() => setDescriptionExpanded((expanded) => !expanded)}
                            className="mt-1 text-xs font-semibold text-white underline underline-offset-2"
                          >{descriptionExpanded ? "Less" : "More"}</button>
                        )}
                      </div>
                    )}
                  </div>

                  {(bufferingId === short.id || autoplayBlockedId === short.id || playbackErrorId === short.id || paused) && (
                    <div className="absolute inset-0 z-10 flex items-center justify-center">
                      {playbackErrorId === short.id ? (
                        <div role="alert" className="rounded-xl bg-black/75 px-4 py-3 text-center text-sm text-white">
                          This Short could not be played.
                          <button type="button" onClick={() => void togglePlayback(short)} className="ml-2 underline">Retry</button>
                        </div>
                      ) : autoplayBlockedId === short.id ? (
                        <button
                          type="button"
                          onClick={() => void togglePlayback(short)}
                          className="flex items-center gap-2 rounded-full bg-black/55 px-5 py-3 text-sm font-medium backdrop-blur"
                        ><Play className="h-5 w-5 fill-current" /> Tap to play</button>
                      ) : bufferingId === short.id ? (
                        <span role="status" className="rounded-full bg-black/45 px-4 py-2 text-sm backdrop-blur">Loading video…</span>
                      ) : (
                        <button
                          type="button"
                          aria-label="Resume video"
                          onClick={() => void togglePlayback(short)}
                          className="flex h-14 w-14 items-center justify-center rounded-full bg-black/45 backdrop-blur"
                        ><Pause className="h-7 w-7" /></button>
                      )}
                    </div>
                  )}
                  <button
                    type="button"
                    aria-label={paused ? "Play video" : "Pause video"}
                    onClick={() => void togglePlayback(short)}
                    className="absolute bottom-4 left-1/2 z-20 hidden -translate-x-1/2 rounded-full bg-black/40 p-3 sm:flex"
                  >
                    {paused ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
                  </button>
                </>
              )}
            </section>
          );
        })}
      </div>

      <div className="pointer-events-none absolute bottom-3 left-1/2 z-30 flex -translate-x-1/2 items-center gap-4 rounded-full bg-black/30 px-4 py-2 text-xs text-white/80 backdrop-blur">
        <span>Swipe up for next</span>
        <MoreHorizontal className="h-4 w-4" />
        <span>{activeIndex + 1} / {shorts.length}</span>
      </div>

      <Sheet
        open={commentsOpen}
        onOpenChange={(open) => {
          setCommentsOpen(open);
          if (!open && activeShort) void refreshEngagement(activeShort.id);
        }}
      >
        <SheetContent
          side="bottom"
          className="z-[80] flex h-[min(76dvh,42rem)] flex-col gap-2 overflow-hidden rounded-t-2xl border-white/10 bg-zinc-950 px-4 pb-[max(env(safe-area-inset-bottom),1rem)] text-white sm:px-6"
        >
          <div className="mx-auto mt-1 h-1 w-10 shrink-0 rounded-full bg-white/30" />
          <SheetTitle className="text-left text-base text-white">Comments · {engagement.commentCount.toLocaleString()}</SheetTitle>
          <SheetDescription className="sr-only">Comments for {activeShort?.title}</SheetDescription>
          {activeShort && (
            <div className="min-h-0 flex-1 overflow-y-auto">
              <CommentsSection key={activeShort.id} videoId={activeShort.id} initialExpanded />
            </div>
          )}
        </SheetContent>
      </Sheet>
      {activeShort && (
        <ReportVideoDialog videoId={activeShort.id} open={reportOpen} onOpenChange={setReportOpen} />
      )}
    </main>
  );
};

export default ShortsPage;
