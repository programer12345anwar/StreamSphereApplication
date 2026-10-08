import { useState } from "react";
import { useQuery, request } from "@/lib/adminApi";

export default function AdminChannelsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useQuery({
    queryKey: ["admin-channels", page],
    queryFn: () => request(`/api/v1/admin/channels?page=${page}&size=10`),
  });

  if (isLoading) return <div>Loading channels…</div>;
  if (error) return <div className="text-red-400">{String(error.message)}</div>;

  const channels = data?.content ?? [];
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Channels</h1>
      {channels.length === 0 ? (
        <div className="text-slate-400">No channels found.</div>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left border-b border-slate-700">
              <th className="py-2 pr-4">Name</th>
              <th className="py-2 pr-4">Description</th>
              <th className="py-2 pr-4">Owner</th>
              <th className="py-2 pr-4">Subscribers</th>
              <th className="py-2 pr-4">Views</th>
            </tr>
          </thead>
          <tbody>
            {channels.map((c) => (
              <tr key={c.id} className="border-b border-slate-800">
                <td className="py-2 pr-4">{c.name}</td>
                <td className="py-2 pr-4 text-slate-400">{c.description}</td>
                <td className="py-2 pr-4 text-slate-400">{c.ownerEmail ?? "—"}</td>
                <td className="py-2 pr-4">{c.totalSubs}</td>
                <td className="py-2 pr-4">{c.totalViews}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      <div className="mt-4 flex items-center gap-3">
        <button disabled={page === 0} onClick={() => setPage((p) => Math.max(0, p - 1))} className="px-3 py-1 rounded bg-slate-700 disabled:opacity-40">
          Prev
        </button>
        <span className="text-slate-400 text-sm">Page {page + 1} of {data?.totalPages ?? 1}</span>
        <button disabled={page + 1 >= (data?.totalPages ?? 1)} onClick={() => setPage((p) => p + 1)} className="px-3 py-1 rounded bg-slate-700 disabled:opacity-40">
          Next
        </button>
      </div>
    </div>
  );
}
