import { Link } from "react-router-dom";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";

interface VideoCardModel {
  id: string;
  title?: string;
  channelName?: string;
  views?: number;
  uploadedAt?: string;
  thumbnailLink?: string;
  thumbnailUrl?: string;
  duration?: string;
}

interface VideoCardProps {
  video: VideoCardModel;
}

export function VideoCard({ video }: VideoCardProps) {
  const formatViews = (v?: number) => {
    if (!v) return "0 views";
    if (v >= 1_000_000) return `${(v / 1_000_000).toFixed(1)}M views`;
    if (v >= 1_000) return `${(v / 1_000).toFixed(1)}K views`;
    return `${v} views`;
  };

  const thumbnail = video.thumbnailLink || video.thumbnailUrl || "";
  const uploadedAt = video.uploadedAt ? new Date(video.uploadedAt).toLocaleDateString() : "Just now";

  return (
    <Link to={`/watch/${video.id}`} className="group block">
      <div className="relative aspect-video overflow-hidden rounded-xl bg-muted">
        {thumbnail ? (
          <img
            src={thumbnail}
            alt={video.title}
            className="h-full w-full object-cover transition-transform group-hover:scale-105"
            loading="lazy"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center bg-muted">
            <svg viewBox="0 0 24 24" className="h-12 w-12 fill-muted-foreground/30">
              <path d="M10 8l6 4-6 4V8z" />
            </svg>
          </div>
        )}
        {video.duration && (
          <span className="absolute bottom-1 right-1 rounded bg-background/90 px-1.5 py-0.5 text-xs font-medium text-foreground">
            {video.duration}
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
        </div>
      </div>
    </Link>
  );
}
