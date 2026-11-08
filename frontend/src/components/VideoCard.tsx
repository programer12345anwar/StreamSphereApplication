import { Link } from "react-router-dom";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { useEffect, useState } from "react";

interface VideoCardModel {
  id: string;
  title?: string;
  channelName?: string;
  views?: number;
  uploadedAt?: string;
  thumbnailLink?: string;
  thumbnailUrl?: string;
  duration?: string;
  durationSeconds?: number;
  thumbnailProcessingStatus?: string;
  tags?: string[];
}

interface VideoCardProps {
  video: VideoCardModel;
}

export function VideoCard({ video }: VideoCardProps) {
  const [imageFailed, setImageFailed] = useState(false);
  const [imageLoaded, setImageLoaded] = useState(false);
  const formatViews = (v?: number) => {
    if (!v) return "0 views";
    if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M views`;
    if (v >= 1_000) return `${(v / 1_000).toFixed(1)}K views`;
    return `${v} views`;
  };

  const thumbnail = video.thumbnailLink || video.thumbnailUrl || "";
  const thumbnailSource = !thumbnail || imageFailed ? "/thumbnail-placeholder.svg" : thumbnail;
  const duration = video.duration || formatDuration(video.durationSeconds);
  const uploadedAt = video.uploadedAt ? new Date(video.uploadedAt).toLocaleDateString() : "Just now";

  useEffect(() => {
    setImageFailed(false);
    setImageLoaded(false);
  }, [thumbnail]);

  return (
    <Link to={`/watch/${video.id}`} className="group block">
      <div className="relative aspect-video overflow-hidden rounded-xl bg-muted">
        {!imageLoaded && <div className="absolute inset-0 animate-pulse bg-muted-foreground/10" aria-hidden="true" />}
        <img
          src={thumbnailSource}
          alt={video.title ? `Thumbnail for ${video.title}` : "Video thumbnail"}
          className={`h-full w-full object-cover transition-transform group-hover:scale-105 ${imageLoaded ? "opacity-100" : "opacity-0"}`}
          loading="lazy"
          decoding="async"
          onLoad={() => setImageLoaded(true)}
          onError={() => {
            if (!imageFailed) {
              setImageFailed(true);
            } else {
              setImageLoaded(true);
            }
          }}
        />
        {duration && (
          <span className="absolute bottom-1 right-1 rounded bg-background/90 px-1.5 py-0.5 text-xs font-medium text-foreground">
            {duration}
          </span>
        )}
        {["PENDING", "PROCESSING"].includes(video.thumbnailProcessingStatus || "") && (
          <span className="absolute left-2 top-2 rounded bg-background/90 px-2 py-1 text-[10px] font-medium text-foreground">
            Generating thumbnail
          </span>
        )}
      </div>
      <div className="mt-3 flex gap-3">
        <Avatar className="h-9 w-9 shrink-0">
          <AvatarFallback className="bg-primary/20 text-primary text-xs">
            {video.channelName?.charAt(0)?.toUpperCase() || "C"}
          </AvatarFallback>
        </Avatar>
        <div className="min-w-0 flex-1">
          <h3 className="line-clamp-2 text-sm font-medium leading-snug text-foreground group-hover:text-foreground/90">
            {video.title}
          </h3>
          <p className="mt-0.5 text-xs text-muted-foreground">{video.channelName}</p>
          <p className="text-xs text-muted-foreground">
            {formatViews(video.views)} • {uploadedAt}
          </p>
          {video.tags && video.tags.length > 0 && (
            <div className="mt-1 flex flex-wrap gap-1">
              {video.tags.slice(0, 3).map((tag, i) => (
                <span key={i} className="inline-block rounded-md bg-secondary px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
                  #{tag}
                </span>
              ))}
            </div>
          )}
        </div>
      </div>
    </Link>
  );
}

function formatDuration(durationSeconds?: number) {
  if (!Number.isFinite(durationSeconds) || !durationSeconds || durationSeconds < 0) return "";
  const seconds = Math.floor(durationSeconds);
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainder = seconds % 60;
  return hours > 0
    ? `${hours}:${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`
    : `${minutes}:${String(remainder).padStart(2, "0")}`;
}
