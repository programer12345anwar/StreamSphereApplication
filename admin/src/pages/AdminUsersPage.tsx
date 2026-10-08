import { useState } from "react";
import { useMutation, useQuery, useQueryClient, request } from "@/lib/adminApi";

export default function AdminUsersPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useQuery({
    queryKey: ["admin-users", page],
    queryFn: () => request(`/api/v1/admin/users?page=${page}&size=10`),
  });
  const qc = useQueryClient();
  const del = useMutation({
    mutationFn: (id) => request(`/api/v1/admin/users/${id}`, { method: "DELETE" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  if (isLoading) return <div>Loading users…</div>;
  if (error) return <div className="text-red-400">{String(error.message)}</div>;

  const users = data?.content ?? [];
  return (
    <div>
      <h1 className="text-2xl font-bold mb-4">Users</h1>
      {users.length === 0 ? (
        <div className="text-slate-400">No users found.</div>
      ) : (
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left border-b border-slate-700">
              <th className="py-2 pr-4">Name</th>
              <th className="py-2 pr-4">Email</th>
              <th className="py-2 pr-4">Role</th>
              <th className="py-2 pr-4">Created</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.id} className="border-b border-slate-800">
                <td className="py-2 pr-4">{u.name}</td>
                <td className="py-2 pr-4">{u.email}</td>
                <td className="py-2 pr-4">{u.role}</td>
                <td className="py-2 pr-4">{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}</td>
                <td className="py-2 pr-4">
                  <button
                    className="text-red-400 hover:underline"
                    onClick={() => {
                      if (confirm(`Delete user ${u.email}?`)) del.mutate(u.id);
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
