import { FormEvent, useState } from "react";
import { Flag } from "lucide-react";
import { reportVideo } from "@/lib/api";
import { toast } from "@/hooks/use-toast";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

const reportReasons = [
  "Spam or misleading",
  "Harassment or bullying",
  "Harmful or dangerous content",
  "Hateful content",
  "Sexual content",
  "Copyright concern",
  "Other",
];

export function ReportVideoDialog({
  videoId,
  open,
  onOpenChange,
}: {
  videoId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}) {
  const [reason, setReason] = useState("");
  const [description, setDescription] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function submitReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!reason || submitting) return;
    setSubmitting(true);
    try {
      const report = await reportVideo(videoId, reason, description.trim());
      if (!Number.isInteger(report?.id) || report.status !== "PENDING") {
        throw new Error("The server did not confirm that the report was saved.");
      }
      toast({ title: "Report submitted", description: `Report #${report.id} was saved for review.` });
      setReason("");
      setDescription("");
      onOpenChange(false);
    } catch (error) {
      toast({
        title: "Could not submit report",
        description: error instanceof Error ? error.message : "Please try again.",
        variant: "destructive",
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(nextOpen) => !submitting && onOpenChange(nextOpen)}>
      <DialogContent className="max-h-[90dvh] w-[calc(100%-2rem)] overflow-y-auto sm:w-full">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2"><Flag className="h-4 w-4" /> Report video</DialogTitle>
          <DialogDescription>Reports are reviewed by the StreamSphere moderation team.</DialogDescription>
        </DialogHeader>
        <form onSubmit={(event) => void submitReport(event)} className="space-y-4">
          <label className="block space-y-2 text-sm font-medium">
            Reason
            <select
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              required
              className="h-10 w-full rounded-md border border-input bg-background px-3 text-sm text-foreground"
            >
              <option value="" disabled>Select a reason</option>
              {reportReasons.map((value) => <option key={value} value={value}>{value}</option>)}
            </select>
          </label>
          <label className="block space-y-2 text-sm font-medium">
            Additional details <span className="font-normal text-muted-foreground">(optional)</span>
            <Textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              maxLength={2000}
              rows={4}
              placeholder="Add context that will help us review this report."
            />
          </label>
          <DialogFooter>
            <Button type="submit" disabled={!reason || submitting}>
              {submitting ? "Submitting…" : "Submit report"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
