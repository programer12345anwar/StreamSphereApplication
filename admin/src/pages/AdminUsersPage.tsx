import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { request } from "@/lib/adminApi";
import { Users, Shield, Trash2, ShieldAlert } from "lucide-react";

export default function AdminUsersPage() {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ["admin-users", page],
    queryFn: () => request(`/api/v1/admin/users?page=${page}&size=10`),
  });

  const roleMut = useMutation({
    mutationFn: ({ id, role }: { id: string; role: string }) =>
      request(`/api/v1/admin/users/${id}/role?role=${role}`, { method: "PUT" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  const delMut = useMutation({
    mutationFn: (id: string) => request(`/api/v1/admin/users/${id}`, { method: "DELETE" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-users"] }),
  });

  if (isLoading) return <div className="text-slate-400 p-6 animate-pulse">Loading users…</div>;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3 mb-8">
        <div className="p-3 bg-blue-500/10 text-blue-400 rounded-xl">
          <Users className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-2xl font-bold bg-gradient-to-r from-slate-100 to-slate-300 bg-clip-text text-transparent">User Management</h1>
          <p className="text-slate-400 text-sm">Manage user roles and accounts</p>
        </div>
      </div>

      <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/60 rounded-2xl overflow-hidden shadow-xl">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-slate-300">
            <thead className="text-xs uppercase bg-slate-800/50 text-slate-400 border-b border-slate-700/50">
              <tr>
                <th className="px-6 py-4 font-semibold">Name</th>
                <th className="px-6 py-4 font-semibold">Email</th>
                <th className="px-6 py-4 font-semibold">Role</th>
                <th className="px-6 py-4 font-semibold text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {data?.content?.map((u: any) => (
                <tr key={u.id} className="hover:bg-slate-800/30 transition-colors">
                  <td className="px-6 py-4 font-medium text-slate-200">{u.name}</td>
                  <td className="px-6 py-4">{u.email}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-1 rounded-full text-xs font-medium border ${u.role === 'ADMIN' ? 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20' : 'bg-slate-800 text-slate-300 border-slate-700'}`}>
                      {u.role}
                    </span>
                  </td>
                  <td className="px-6 py-4 flex gap-2 justify-end">
                    <button
                      onClick={() => roleMut.mutate({ id: u.id, role: u.role === "ADMIN" ? "USER" : "ADMIN" })}
                      className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs font-medium transition-colors flex items-center gap-1.5"
                    >
                      {u.role === "ADMIN" ? <Shield className="w-3.5 h-3.5" /> : <ShieldAlert className="w-3.5 h-3.5" />}
                      Toggle Role
                    </button>
                    <button
                      onClick={() => {
                        if (confirm(`Delete user ${u.email}?`)) delMut.mutate(u.id);
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
