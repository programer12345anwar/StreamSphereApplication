import { Layout } from "@/components/Layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/contexts/AuthContext";
import { getMyChannel, uploadVideo } from "@/lib/api";
import { toast } from "@/hooks/use-toast";
import { Film, Upload } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";

const UploadPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const fileRef = useRef(null);
  const [videoFile, setVideoFile] = useState(null);
  const [form, setForm] = useState({ name: "", description: "", tags: "" });
  const [loading, setLoading] = useState(false);
  const [channel, setChannel] = useState(null);
  const [channelLoading, setChannelLoading] = useState(true);

  const set = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  useEffect(() => {
    let mounted = true;

    const loadChannel = async () => {
      if (!user?.email) {
        if (mounted) {
          setChannelLoading(false);
        }
        return;
      }

      setChannelLoading(true);
      try {
        const myChannel = await getMyChannel(user.email);
        if (mounted) {
          setChannel(myChannel || null);
        }
      } catch {
        if (mounted) {
          setChannel(null);
        }
      } finally {
        if (mounted) {
          setChannelLoading(false);
        }
      }
    };

    loadChannel();

    return () => {
      mounted = false;
    };
  }, [user?.email]);

  const handleUpload = async (e) => {
    e.preventDefault();

    if (!videoFile) {
      toast({ title: "Select a video file", variant: "destructive" });
      return;
    }

    if (!channel?.id) {
      toast({
        title: "Channel required",
        description: "Create a channel first before uploading.",
        variant: "destructive",
      });
      return;
    }

    setLoading(true);
    try {
      await uploadVideo(channel.id, videoFile, {
        name: form.name.trim(),
        description: form.description.trim(),
        tags: form.tags
          .split(",")
          .map((t) => t.trim())
          .filter(Boolean),
      });
      toast({ title: "Video uploaded successfully" });
      navigate("/");
    } catch (err) {
      toast({ title: "Upload failed", description: err.message, variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="mx-auto max-w-2xl p-6">
        <h1 className="text-2xl font-semibold text-foreground">Upload video</h1>
        <p className="mt-1 text-sm text-muted-foreground">Share your video with the world</p>

        {channelLoading && <p className="mt-4 text-sm text-muted-foreground">Fetching your channel...</p>}

        {!channelLoading && !channel && (
          <div className="mt-6 rounded-xl border border-border bg-card p-6">
            <div className="flex items-center gap-3">
              <Film className="h-6 w-6 text-muted-foreground" />
              <div>
                <p className="text-sm font-medium text-foreground">No channel found for your account</p>
                <p className="text-sm text-muted-foreground">
                  Create your channel first, then come back here to upload videos.
                </p>
              </div>
            </div>
            <div className="mt-4">
              <Button asChild className="rounded-full">
                <Link to="/channel/create">Create channel</Link>
              </Button>
            </div>
          </div>
        )}

        {!channelLoading && channel && (
          <form onSubmit={handleUpload} className="mt-6 space-y-5">
            <div className="rounded-lg border border-border bg-card p-4">
              <p className="text-sm text-muted-foreground">Uploading to channel</p>
              <p className="text-sm font-medium text-foreground">{channel.name}</p>
              <p className="text-xs text-muted-foreground">{channel.id}</p>
            </div>

            <div
              onClick={() => fileRef.current?.click()}
              className="flex cursor-pointer flex-col items-center justify-center gap-3 rounded-xl border-2 border-dashed border-border py-12 transition-colors hover:border-muted-foreground"
            >
              <div className="flex h-14 w-14 items-center justify-center rounded-full bg-muted">
                <Upload className="h-6 w-6 text-muted-foreground" />
              </div>
              {videoFile ? (
                <p className="text-sm font-medium text-foreground">{videoFile.name}</p>
              ) : (
                <>
                  <p className="text-sm text-foreground">Click to select a video file</p>
                  <p className="text-xs text-muted-foreground">MP4, WebM or OGG</p>
                </>
              )}
              <input
                ref={fileRef}
                type="file"
                accept="video/*"
                className="hidden"
                onChange={(e) => setVideoFile(e.target.files?.[0] || null)}
              />
            </div>

            <div className="space-y-1.5">
              <Label>Title</Label>
              <Input
                value={form.name}
                onChange={(e) => set("name", e.target.value)}
                required
                placeholder="Add a title that describes your video"
              />
            </div>
            <div className="space-y-1.5">
              <Label>Description</Label>
              <Textarea
                value={form.description}
                onChange={(e) => set("description", e.target.value)}
                placeholder="Tell viewers about your video"
                rows={4}
              />
            </div>
            <div className="space-y-1.5">
              <Label>Tags</Label>
              <Input
                value={form.tags}
                onChange={(e) => set("tags", e.target.value)}
                placeholder="springboot, java, tutorial (comma separated)"
              />
            </div>

            <div className="flex justify-end gap-3">
              <Button type="button" variant="ghost" onClick={() => navigate("/")}>Cancel</Button>
              <Button type="submit" className="rounded-full px-6" disabled={loading}>
                {loading ? "Uploading..." : "Upload"}
              </Button>
            </div>
          </form>
        )}
      </div>
    </Layout>
  );
};

export default UploadPage;
