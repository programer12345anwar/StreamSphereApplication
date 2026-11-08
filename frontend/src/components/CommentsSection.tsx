import { FormEvent, useEffect, useRef, useState } from "react";
import { Heart, MessageCircle, Reply, Send } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { addCommentToVideo, getCommentReplies, getVideoComments, toggleCommentLike } from "@/lib/api";
import { toast } from "@/hooks/use-toast";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Link } from "react-router-dom";

interface CommentItem {
  id: string;
  text: string;
  authorName: string;
  createdAt?: string;
  parentId?: string | null;
  likeCount: number;
  likedByCurrentUser: boolean;
}

interface CommentPage {
  content: CommentItem[];
  totalElements: number;
  totalPages: number;
  number: number;
}

const PAGE_SIZE = 20;

function normalizeComment(value: any): CommentItem | null {
  if (!value || typeof value !== "object" || value.id == null || typeof value.text !== "string") {
    return null;
  }
  return {
    id: String(value.id),
    text: value.text,
    authorName: value.authorName || value.user?.name || "StreamSphere user",
    createdAt: value.createdAt,
    parentId: value.parentId ?? value.parentComment?.id ?? null,
    likeCount: Number(value.likeCount ?? 0),
    likedByCurrentUser: Boolean(value.likedByCurrentUser),
  };
}

function normalizePage(response: any, page: number): CommentPage {
  const content = Array.isArray(response) ? response : response?.content;
  return {
    content: Array.isArray(content)
      ? content.map(normalizeComment).filter((item): item is CommentItem => item !== null)
      : [],
    totalElements: Number(response?.totalElements ?? content?.length ?? 0),
    totalPages: Number(response?.totalPages ?? (content?.length ? page + 1 : 0)),
    number: Number(response?.number ?? page),
  };
}

function formatCommentDate(value?: string) {
  if (!value) return "Recently";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Recently";
  const elapsed = Math.max(0, Date.now() - date.getTime());
  const minutes = Math.floor(elapsed / 60_000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return date.toLocaleDateString();
}

function CommentsSection({ videoId, initialExpanded = false }: { videoId: string; initialExpanded?: boolean }) {
  const { user } = useAuth();
  const [comments, setComments] = useState<CommentItem[]>([]);
  const [totalComments, setTotalComments] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [draft, setDraft] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [replyingTo, setReplyingTo] = useState<string | null>(null);
  const [replyDraft, setReplyDraft] = useState("");
  const [repliesByComment, setRepliesByComment] = useState<Record<string, CommentItem[]>>({});
  const [replyStatus, setReplyStatus] = useState<Record<string, "loading" | "loaded" | "error">>({});
  const [likingCommentIds, setLikingCommentIds] = useState<Set<string>>(new Set());
  const activeVideoId = useRef(videoId);

  async function loadFirstPage() {
    const requestVideoId = videoId;
    setLoading(true);
    setError("");
    try {
      const page = normalizePage(await getVideoComments(requestVideoId, 0, PAGE_SIZE), 0);
      if (activeVideoId.current !== requestVideoId) return;
      setComments(page.content);
      setTotalComments(page.totalElements);
      setHasMore(page.number + 1 < page.totalPages);
    } catch {
      if (activeVideoId.current === requestVideoId) {
        setError("Comments are temporarily unavailable.");
      }
    } finally {
      if (activeVideoId.current === requestVideoId) setLoading(false);
    }
  }

  useEffect(() => {
    activeVideoId.current = videoId;
    setComments([]);
    setTotalComments(0);
    setExpanded(false);
    setLoading(true);
    setLoadingMore(false);
    setDraft("");
    setReplyDraft("");
    setReplyingTo(null);
    setRepliesByComment({});
    setReplyStatus({});
    void loadFirstPage();
  }, [videoId]);

  useEffect(() => {
    if (initialExpanded) setExpanded(true);
  }, [initialExpanded]);

  async function handleCommentLike(comment: CommentItem) {
    if (!user?.id) {
      toast({ title: "Sign in to like comments" });
      return;
    }
    if (likingCommentIds.has(comment.id)) return;
    setLikingCommentIds((previous) => new Set(previous).add(comment.id));
    try {
      const result = await toggleCommentLike(comment.id);
      const update = (item: CommentItem) => item.id === comment.id
        ? { ...item, likeCount: result.likeCount, likedByCurrentUser: result.likedByCurrentUser }
        : item;
      setComments((previous) => previous.map(update));
      setRepliesByComment((previous) => Object.fromEntries(
        Object.entries(previous).map(([parentId, replies]) => [parentId, replies.map(update)]),
      ));
    } catch (error) {
      toast({
        title: "Could not update comment like",
        description: error instanceof Error ? error.message : undefined,
        variant: "destructive",
      });
    } finally {
      setLikingCommentIds((previous) => {
        const next = new Set(previous);
        next.delete(comment.id);
        return next;
      });
    }
  }

  async function loadMoreComments() {
    if (loadingMore || !hasMore) return;
    const requestVideoId = videoId;
    setLoadingMore(true);
    setError("");
    try {
      const nextPage = Math.ceil(comments.length / PAGE_SIZE);
      const page = normalizePage(await getVideoComments(requestVideoId, nextPage, PAGE_SIZE), nextPage);
      if (activeVideoId.current !== requestVideoId) return;
      setComments((previous) => {
        const existingIds = new Set(previous.map((comment) => comment.id));
        return [...previous, ...page.content.filter((comment) => !existingIds.has(comment.id))];
      });
      setTotalComments(page.totalElements);
      setHasMore(page.number + 1 < page.totalPages);
    } catch {
      if (activeVideoId.current === requestVideoId) {
        setError("Could not load more comments. Please try again.");
      }
    } finally {
      if (activeVideoId.current === requestVideoId) setLoadingMore(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || submitting) return;
    if (!user?.id) {
      toast({ title: "Sign in to join the conversation" });
      return;
    }

    setSubmitting(true);
    try {
      await addCommentToVideo(videoId, String(user.id), text);
      setDraft("");
      await loadFirstPage();
      setExpanded(true);
    } catch (submitError) {
      toast({
        title: "Could not post your comment",
        description: submitError instanceof Error ? submitError.message : undefined,
        variant: "destructive",
      });
    } finally {
      setSubmitting(false);
    }
  }

  async function loadReplies(commentId: string) {
    const requestVideoId = videoId;
    if (replyStatus[commentId] === "loading") return;
    setReplyStatus((previous) => ({ ...previous, [commentId]: "loading" }));
    try {
      const page = normalizePage(await getCommentReplies(commentId, 0, PAGE_SIZE), 0);
      if (activeVideoId.current !== requestVideoId) return;
      setRepliesByComment((previous) => ({ ...previous, [commentId]: page.content }));
      setReplyStatus((previous) => ({ ...previous, [commentId]: "loaded" }));
    } catch {
      if (activeVideoId.current === requestVideoId) {
        setReplyStatus((previous) => ({ ...previous, [commentId]: "error" }));
      }
    }
  }

  async function toggleReplies(commentId: string) {
    if (replyingTo === commentId) {
      setReplyingTo(null);
      return;
    }
    setReplyingTo(commentId);
    if (replyStatus[commentId] === "loaded") return;
    await loadReplies(commentId);
  }

  async function submitReply(event: FormEvent<HTMLFormElement>, parentId: string) {
    event.preventDefault();
    const text = replyDraft.trim();
    if (!text) return;
    if (!user?.id) {
      toast({ title: "Sign in to reply" });
      return;
    }
    try {
      await addCommentToVideo(videoId, String(user.id), text, parentId);
      setReplyDraft("");
      setReplyStatus((previous) => ({ ...previous, [parentId]: "loading" }));
      const page = normalizePage(await getCommentReplies(parentId, 0, PAGE_SIZE), 0);
      setRepliesByComment((previous) => ({ ...previous, [parentId]: page.content }));
      setReplyStatus((previous) => ({ ...previous, [parentId]: "loaded" }));
    } catch {
      toast({ title: "Could not post your reply", variant: "destructive" });
    }
  }

  const preview = comments.slice(0, 1);

  return (
    <section className="mt-5 min-w-0 rounded-xl border border-border bg-card p-4 text-card-foreground sm:mt-6 sm:p-5" aria-labelledby="comments-heading">
      <button
        type="button"
        className="flex min-h-11 w-full items-center gap-2 text-left"
        aria-expanded={expanded}
        aria-controls={expanded ? "video-comments-list" : undefined}
        onClick={() => setExpanded((open) => !open)}
      >
        <MessageCircle className="h-5 w-5" />
        <h2 id="comments-heading" className="text-base font-semibold">Comments</h2>
        <span className="text-sm text-muted-foreground">
          {loading ? "…" : totalComments.toLocaleString()}
        </span>
        <span className="ml-auto text-sm font-medium text-primary">
          {expanded ? "Collapse" : totalComments > 1 ? "View all" : "Join in"}
        </span>
      </button>

      {error && (
        <div className="mt-3 flex flex-wrap items-center justify-between gap-2 text-sm text-muted-foreground" role="status">
          <span>{error}</span>
          <Button type="button" variant="outline" size="sm" onClick={() => void loadFirstPage()}>Retry</Button>
        </div>
      )}
      {!loading && !error && totalComments === 0 && (
        <p className="mt-3 text-sm text-muted-foreground">No comments yet. Start the conversation.</p>
      )}

      {!loading && (expanded || preview.length > 0) && (
        <div
          id="video-comments-list"
          className={`mt-3 space-y-3 ${expanded ? "max-h-[50vh] overflow-y-auto overscroll-contain border-t pt-4" : ""}`}
        >
          {expanded && (
            user?.id ? (
              <form onSubmit={(event) => void handleSubmit(event)} className="flex gap-2">
                <Input
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder="Add a comment"
                  maxLength={2000}
                  aria-label="Add a comment"
                />
                <Button type="submit" size="icon" aria-label="Post comment" disabled={!draft.trim() || submitting}>
                  <Send className="h-4 w-4" />
                </Button>
              </form>
            ) : (
              <div className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-muted p-3">
                <p className="text-sm text-muted-foreground">Sign in to join the conversation.</p>
                <Button asChild size="sm" variant="outline"><Link to="/login">Sign in</Link></Button>
              </div>
            )
          )}
          {(expanded ? comments : preview).map((comment) => (
            <article key={comment.id} className="flex min-w-0 gap-3">
              <Avatar className="mt-0.5 h-9 w-9 shrink-0">
                <AvatarFallback className="bg-primary/15 text-primary">
                  {comment.authorName.charAt(0).toUpperCase()}
                </AvatarFallback>
              </Avatar>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-baseline gap-x-2">
                  <span className="text-sm font-medium">{comment.authorName}</span>
                  <time className="text-xs text-muted-foreground" dateTime={comment.createdAt}>
                    {formatCommentDate(comment.createdAt)}
                  </time>
                </div>
                <p className="mt-1 whitespace-pre-wrap break-words text-sm leading-relaxed">{comment.text}</p>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  aria-label={comment.likedByCurrentUser ? "Unlike comment" : "Like comment"}
                  aria-pressed={comment.likedByCurrentUser}
                  disabled={likingCommentIds.has(comment.id)}
                  onClick={() => void handleCommentLike(comment)}
                  className={`mt-1 min-h-9 px-2 ${comment.likedByCurrentUser ? "text-rose-500" : "text-muted-foreground"}`}
                >
                  <Heart className={`mr-1 h-4 w-4 ${comment.likedByCurrentUser ? "fill-current" : ""}`} />
                  {comment.likeCount.toLocaleString()}
                </Button>
                {expanded && (
                  <>
                    <Button
                      type="button"
                      variant="ghost"
                      size="sm"
                      className="mt-1 min-h-9 px-2 text-muted-foreground"
                      onClick={() => void toggleReplies(comment.id)}
                      aria-expanded={replyingTo === comment.id}
                    >
                      <Reply className="mr-1 h-4 w-4" />
                      {replyingTo === comment.id ? "Hide replies" : "Reply"}
                    </Button>
                    {replyingTo === comment.id && (
                      <div className="mt-2 space-y-3 border-l pl-3 sm:pl-4">
                        {replyStatus[comment.id] === "loading" && (
                          <p className="text-xs text-muted-foreground" role="status">Loading replies…</p>
                        )}
                        {replyStatus[comment.id] === "error" && (
                          <div className="flex items-center gap-2 text-xs text-muted-foreground">
                            <span>Replies are unavailable.</span>
                            <button type="button" className="text-primary underline" onClick={() => void loadReplies(comment.id)}>Retry</button>
                          </div>
                        )}
                        {(repliesByComment[comment.id] || []).map((reply) => (
                          <div key={reply.id} className="flex gap-2">
                            <Avatar className="h-7 w-7 shrink-0">
                              <AvatarFallback className="bg-muted text-xs">{reply.authorName.charAt(0).toUpperCase()}</AvatarFallback>
                            </Avatar>
                            <div className="min-w-0">
                              <p className="text-xs font-medium">{reply.authorName} <span className="ml-1 font-normal text-muted-foreground">{formatCommentDate(reply.createdAt)}</span></p>
                              <p className="mt-1 break-words text-sm">{reply.text}</p>
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                aria-label={reply.likedByCurrentUser ? "Unlike comment" : "Like comment"}
                                aria-pressed={reply.likedByCurrentUser}
                                disabled={likingCommentIds.has(reply.id)}
                                onClick={() => void handleCommentLike(reply)}
                                className={`mt-1 min-h-8 px-2 text-xs ${reply.likedByCurrentUser ? "text-rose-500" : "text-muted-foreground"}`}
                              >
                                <Heart className={`mr-1 h-3.5 w-3.5 ${reply.likedByCurrentUser ? "fill-current" : ""}`} />
                                {reply.likeCount.toLocaleString()}
                              </Button>
                            </div>
                          </div>
                        ))}
                        {user?.id ? (
                          <form className="flex gap-2" onSubmit={(event) => void submitReply(event, comment.id)}>
                            <Input
                              value={replyDraft}
                              onChange={(event) => setReplyDraft(event.target.value)}
                              placeholder="Write a reply"
                              maxLength={2000}
                              aria-label={`Reply to ${comment.authorName}`}
                            />
                            <Button type="submit" size="icon" aria-label="Post reply" disabled={!replyDraft.trim()}>
                              <Send className="h-4 w-4" />
                            </Button>
                          </form>
                        ) : (
                          <p className="text-xs text-muted-foreground">
                            <Link to="/login" className="font-medium text-primary underline">Sign in</Link> to reply.
                          </p>
                        )}
                      </div>
                    )}
                  </>
                )}
              </div>
            </article>
          ))}
          {expanded && hasMore && (
            <Button type="button" variant="outline" className="w-full" disabled={loadingMore} onClick={() => void loadMoreComments()}>
              {loadingMore ? "Loading comments…" : "Load more comments"}
            </Button>
          )}
        </div>
      )}

    </section>
  );
}

export default CommentsSection;
