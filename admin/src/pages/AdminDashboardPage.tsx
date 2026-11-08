import { useEffect, useState } from "react";
import { getApiBaseUrl, request, getToken } from "@/lib/adminApi";
import { Client } from "@stomp/stompjs";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import { Users, Video, Tv, Eye, Activity } from "lucide-react";

export default function AdminDashboardPage() {
  const [data, setData] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // 1. Initial fetch
    request("/api/v1/admin/analytics")
      .then((res) => {
        setData(res);
        setHistory([{ time: new Date().toLocaleTimeString(), ...res }]);
      })
      .catch((err) => setError(String(err.message)));

    // 2. WebSocket setup
    const token = getToken();
    let client: Client | undefined;
    let disposed = false;
    getApiBaseUrl()
      .then((apiBaseUrl) => {
        if (disposed) return;
        const wsUrl = apiBaseUrl.replace(/^http/, "ws");
        client = new Client({
          brokerURL: `${wsUrl}/api/v1/central/ws`,
          connectHeaders: { Authorization: `Bearer ${token}` },
          reconnectDelay: 5000,
          heartbeatIncoming: 4000,
          heartbeatOutgoing: 4000,
        });

        client.onConnect = () => {
          client?.subscribe("/topic/admin/analytics", (message) => {
            if (message.body) {
              const newAnalytics = JSON.parse(message.body);
              setData(newAnalytics);
              setHistory((prev) => {
                const updated = [...prev, { time: new Date().toLocaleTimeString(), ...newAnalytics }];
                if (updated.length > 20) updated.shift();
                return updated;
              });
            }
          });
        };

        client.onStompError = (frame) => console.error("STOMP error:", frame.headers["message"]);
        client.activate();
      })
      .catch((err) => setError(String(err.message)));

    return () => {
      disposed = true;
      void client?.deactivate();
    };
  }, []);

  if (error) return <div className="text-red-400 p-6 bg-slate-900 rounded-xl m-6 border border-red-900">{error}</div>;
  if (!data) return <div className="p-6 text-slate-400 flex items-center gap-2"><Activity className="animate-spin" /> Loading real-time dashboard…</div>;

  const metrics = [
    { label: "Total Users", value: data.totalUsers, icon: Users, color: "text-blue-400", bg: "bg-blue-400/10" },
    { label: "Total Videos", value: data.totalVideos, icon: Video, color: "text-purple-400", bg: "bg-purple-400/10" },
    { label: "Total Channels", value: data.totalChannels, icon: Tv, color: "text-emerald-400", bg: "bg-emerald-400/10" },
    { label: "Total Views", value: data.totalViews, icon: Eye, color: "text-amber-400", bg: "bg-amber-400/10" },
  ];

  return (
    <div className="min-w-0 space-y-5 sm:space-y-6">
      <div className="mb-6 flex min-w-0 flex-wrap items-center justify-between gap-3 sm:mb-8">
        <div className="min-w-0">
          <h1 className="break-words bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-2xl font-bold text-transparent sm:text-3xl">Overview</h1>
          <p className="text-slate-400 mt-1">Real-time statistics across the StreamSphere network</p>
        </div>
        <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-500/10 text-emerald-400 rounded-full text-sm border border-emerald-500/20 shadow-[0_0_15px_rgba(16,185,129,0.1)]">
          <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          Live
        </div>
      </div>

      <div className="grid min-w-0 grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 lg:gap-6">
        {metrics.map((m) => (
          <div key={m.label} className="relative min-w-0 overflow-hidden rounded-2xl border border-slate-700/50 bg-slate-800/50 p-4 backdrop-blur-xl transition-transform hover:-translate-y-1 hover:shadow-xl hover:shadow-indigo-500/10 sm:p-6">
            <div className="relative z-10 flex min-w-0 items-center gap-3 sm:gap-4">
              <div className={`p-3 rounded-xl ${m.bg}`}>
                <m.icon className={`w-6 h-6 ${m.color}`} />
              </div>
              <div>
                <div className="text-sm font-medium text-slate-400">{m.label}</div>
                <div className="break-all text-2xl font-bold text-slate-100 sm:text-3xl">{m.value?.toLocaleString() ?? 0}</div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-6 min-w-0 overflow-hidden rounded-2xl border border-slate-700/50 bg-slate-800/50 p-4 backdrop-blur-xl sm:mt-8 sm:p-6">
        <h2 className="mb-4 flex min-w-0 items-center gap-2 text-lg font-semibold sm:mb-6 sm:text-xl">
          <Activity className="w-5 h-5 text-indigo-400" /> Live Traffic Activity
        </h2>
        <div className="h-64 min-w-0 w-full sm:h-80">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={history} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="time" stroke="#94a3b8" fontSize={10} tickMargin={6} minTickGap={20} />
              <YAxis yAxisId="left" stroke="#94a3b8" fontSize={10} width={34} />
              <YAxis yAxisId="right" orientation="right" stroke="#94a3b8" fontSize={10} width={34} />
              <Tooltip 
                contentStyle={{ backgroundColor: "#1e293b", borderColor: "#334155", borderRadius: "8px" }}
                itemStyle={{ color: "#e2e8f0" }}
              />
              <Line yAxisId="left" type="monotone" dataKey="totalViews" stroke="#f59e0b" strokeWidth={3} dot={false} activeDot={{ r: 6 }} name="Views" />
              <Line yAxisId="right" type="monotone" dataKey="totalVideos" stroke="#a855f7" strokeWidth={3} dot={false} name="Videos" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="mt-5 grid min-w-0 grid-cols-1 gap-4 sm:mt-6 sm:gap-6">
        <div className="min-w-0 overflow-hidden rounded-2xl border border-slate-700/50 bg-slate-800/50 p-4 backdrop-blur-xl sm:p-6">
          <h2 className="mb-4 flex min-w-0 items-center gap-2 text-lg font-semibold sm:mb-6 sm:text-xl">
            <Users className="w-5 h-5 text-blue-400" /> Platform Growth
          </h2>
          <div className="h-56 min-w-0 w-full sm:h-64">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={history} margin={{ top: 8, right: 12, bottom: 4, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="time" stroke="#94a3b8" fontSize={10} tickMargin={6} minTickGap={20} />
                <YAxis stroke="#94a3b8" fontSize={10} width={34} />
                <Tooltip 
                  contentStyle={{ backgroundColor: "#1e293b", borderColor: "#334155", borderRadius: "8px" }}
                />
                <Line type="stepAfter" dataKey="totalUsers" stroke="#3b82f6" strokeWidth={3} dot={false} name="Users" />
                <Line type="stepAfter" dataKey="totalChannels" stroke="#10b981" strokeWidth={3} dot={false} name="Channels" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
