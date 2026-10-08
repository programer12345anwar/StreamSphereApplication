import { useQuery } from "@tanstack/react-query";
import { request } from "@/lib/adminApi";

export default function AdminDashboardPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ["admin-analytics"],
    queryFn: () => request("/api/v1/admin/analytics"),
  });

  if (isLoading) return <div>Loading dashboard…</div>;
  if (error) return <div className="text-red-400">{String(error.message)}</div>;

  const metrics = [
    { label: "Total Users", value: data?.totalUsers },
    { label: "Total Videos", value: data?.totalVideos },
    { label: "Total Channels", value: data?.totalChannels },
    { label: "Total Views", value: data?.totalViews },
  ];

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">Dashboard</h1>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {metrics.map((m) => (
          <div key={m.label} className="bg-slate-800 rounded-lg p-4">
            <div className="text-3xl font-bold">{m.value ?? 0}</div>
            <div className="text-slate-400 text-sm">{m.label}</div>
          </div>
        ))}
      </div>
    </div>
  );
}
