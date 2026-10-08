import { useState } from "react";
import { useMutation, useQuery, useQueryClient, request } from "@/lib/adminApi";


export default function AdminVideosPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useQuery({
    queryKey: ["admin-videos", page],
    queryFn: () => request(`/api/v1/admin/videos?page=${page}&size=10`),
  });
  const qc = useQueryClient();
  const del = useMutation({
    mutationFn: (id) => request(`/api/v1/admin/videos/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-videos"] }),
  });

  if (isLoading) return <div>Loading videos…</div>;
  if (error) return <div className="text-red-400">{String(error.message)}</div>;

  const videos = data?.content ?? [];
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Videos</h1>
      {videos.length === 0 ? (
        <div className="text-slate-400">No videos found.</div>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left border-b border-slate-700">
              <th className="py-2 pr-4">Title</th>
              <th className="py-2 pr-4">ID</th>
              <th className="py-2 pr-4">Views</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {videos.map((v) => (
              <tr key={v.id} className="border-b border-slate-800">
                <td className="py-2 pr-4">{v.title ?? v.name}</td>
                <td className="py-2 pr-4 text-slate-400">{v.id}</td>
                <td className="py-2 pr-4">{v.views}</td>
                <td className="py-2 pr-4">
                  <button
                    className="text-red-400 hover:underline"
                    onClick={() => {
                      if (confirm(`Delete video ${v.title ?? v.name}?`)) del.mutate(v.id);
                    }}
                  >
                    Delete
                  </button>
                </td>
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
