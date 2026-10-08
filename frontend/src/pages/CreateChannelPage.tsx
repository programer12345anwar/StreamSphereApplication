import { Layout } from "@/components/Layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { useAuth } from "@/contexts/AuthContext";
import { createChannel } from "@/lib/api";
import { toast } from "@/hooks/use-toast";
import { useState } from "react";
import { useNavigate } from "react-router-dom";

const CreateChannelPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [channelName, setChannelName] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);

  const handleCreate = async (e) => {
    e.preventDefault();

    if (!user?.email) {
      toast({
        title: "Session issue",
        description: "Please sign in again.",
        variant: "destructive",
      });
      navigate("/login");
      return;
    }

    setLoading(true);
    try {
      await createChannel({
        userEmail: user.email,
        channelName: channelName.trim(),
        description: description.trim(),
      });
      toast({ title: "Channel created successfully" });
      navigate("/");
    } catch (err) {
      toast({ title: "Failed to create channel", description: err.message, variant: "destructive" });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="mx-auto max-w-lg p-6">
        <h1 className="text-2xl font-semibold text-foreground">Create your channel</h1>
        <p className="mt-1 text-sm text-muted-foreground">Start sharing content with the world</p>

        <form onSubmit={handleCreate} className="mt-6 space-y-4">
          <div className="space-y-1.5">
            <Label>Channel Name</Label>
            <Input
              value={channelName}
              onChange={(e) => setChannelName(e.target.value)}
              required
              placeholder="TechTalks by Anwar"
            />
          </div>
          <div className="space-y-1.5">
            <Label>Description</Label>
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="What's your channel about?"
              rows={4}
            />
          </div>
          <div className="flex justify-end gap-3">
            <Button type="button" variant="ghost" onClick={() => navigate("/")}>
              Cancel
            </Button>
            <Button type="submit" className="rounded-full px-6" disabled={loading}>
              {loading ? "Creating..." : "Create Channel"}
            </Button>
          </div>
        </form>
      </div>
    </Layout>
  );
};

export default CreateChannelPage;
