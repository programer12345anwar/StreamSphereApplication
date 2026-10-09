import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { request } from "@/lib/adminApi";
import { TvMinimalPlay, Heart, PlaySquare, Mail } from "lucide-react";

export default function AdminChannelsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useQuery({
    queryKey: ["admin-channels", page],
    queryFn: () => request(`/api/v1/admin/channels?page=${page}&size=10`),
  });

  if (isLoading) return <div className="text-slate-400 p-6 animate-pulse">Loading channels…</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3 mb-8">
        <div className="p-3 bg-emerald-500/10 text-emerald-400 rounded-xl">
          <TvMinimalPlay className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold bg-gradient-to-r from-slate-100 to-slate-300 bg-clip-text text-transparent">Channel Directory</h1>
          <p className="text-slate-400 text-sm">Browse all platform channels</p>
        </div>
      </div>

      <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/60 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="text-xs uppercase bg-slate-800/50 text-slate-400 border-b border-slate-700/50">
              <tr>
                <th className="px-6 py-4 font-semibold">Channel Name</th>
                <th className="px-6 py-4 font-semibold">Owner</th>
                <th className="px-6 py-4 font-semibold">Stats</th>
                <th className="px-6 py-4 font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {data?.content?.map((c: any) => (
                <tr key={c.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-6 py-4">
                    <div className="font-medium text-slate-200">{c.name}</div>
                    <div className="text-xs text-slate-500 mt-0.5 line-clamp-1">{c.description || "No description"}</div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-1.5 text-slate-300">
                      <Mail className="w-3.5 h-3.5 text-slate-500" />
                      {c.ownerEmail}
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <div className="flex items-center gap-4 text-xs">
                      <div className="flex items-center gap-1 text-pink-400/80">
                        <Heart className="w-3.5 h-3.5" />
                        {c.totalSubs?.toLocaleString() || 0}
                      </div>
                      <div className="flex items-center gap-1 text-sky-400/80">
                        <PlaySquare className="w-3.5 h-3.5" />
                        {c.totalViews?.toLocaleString() || 0}
                      </div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    {c.monetized ? (
                      <span className="px-2.5 py-1 rounded-full text-xs font-medium border bg-amber-500/10 text-amber-400 border-amber-500/20">Monetized</span>
                    ) : (
                      <span className="px-2.5 py-1 rounded-full text-xs font-medium border bg-slate-800 text-slate-400 border-slate-700">Standard</span>
                    )}
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
