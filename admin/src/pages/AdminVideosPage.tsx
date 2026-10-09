import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { request } from "@/lib/adminApi";
import { Clapperboard, Trash2, Video, User } from "lucide-react";

export default function AdminVideosPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["admin-videos", page],
    queryFn: () => request(`/api/v1/admin/videos?page=${page}&size=10`),
  });

  const delMut = useMutation({
    mutationFn: (id: string) => request(`/api/v1/admin/videos/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-videos"] }),
  });

  if (isLoading) return <div className="text-slate-400 p-6 animate-pulse">Loading videos…</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3 mb-8">
        <div className="p-3 bg-purple-500/10 text-purple-400 rounded-xl">
          <Clapperboard className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold bg-gradient-to-r from-slate-100 to-slate-300 bg-clip-text text-transparent">Video Moderation</h1>
          <p className="text-slate-400 text-sm">Review and manage uploaded content</p>
        </div>
      </div>

      <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/60 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="text-xs uppercase bg-slate-800/50 text-slate-400 border-b border-slate-700/50">
              <tr>
                <th className="px-6 py-4 font-semibold">Video Details</th>
                <th className="px-6 py-4 font-semibold">Channel</th>
                <th className="px-6 py-4 font-semibold">Views</th>
                <th className="px-6 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {data?.content?.map((v: any) => (
                <tr key={v.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      {v.thumbnailLink ? (
                        <img src={v.thumbnailLink} alt="thumb" className="w-16 h-10 object-cover rounded shadow-md" />
                      ) : (
                        <div className="w-16 h-10 bg-slate-800 rounded flex items-center justify-center">
                          <Video className="w-4 h-4 text-slate-500" />
                        </div>
                      )}
                      <div>
                        <div className="font-medium text-slate-200 line-clamp-1">{v.title}</div>
                        <div className="text-xs text-slate-500 mt-0.5">{new Date(v.uploadedAt).toLocaleDateString()}</div>
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1.5">
                      <User className="w-3.5 h-3.5 text-slate-400" />
                      {v.channelName}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 bg-slate-800 rounded-md text-xs font-mono">{v.views?.toLocaleString()}</span>
                  </td>
                  <td className="px-6 py-4 flex gap-2 justify-end">
                    <button
                      onClick={() => {
                        if (confirm(`Delete video "${v.title}"?`)) delMut.mutate(v.id);
                      }}
                      className="px-3 py-1.5 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        
        <div className="flex gap-4 p-4 border-t border-slate-800/60 items-center justify-between bg-slate-800/20">
          <span className="text-sm text-slate-400">Page {data?.number + 1} of {data?.totalPages}</span>
          <div className="flex gap-2">
            <button
              disabled={data?.first}
              onClick={() => setPage(p => Math.max(0, p - 1))}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-slate-300 rounded-lg text-sm font-medium transition-colors"
            >
              Previous
            </button>
            <button
              disabled={data?.last}
              onClick={() => setPage(p => p + 1)}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-slate-300 rounded-lg text-sm font-medium transition-colors"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
