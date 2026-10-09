import { useEffect, useState } from "react";
import { request, getToken } from "@/lib/adminApi";
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
    const wsUrl = (import.meta.env.VITE_API_GATEWAY_URL ?? (typeof window !== "undefined" ? `${window.location.protocol}//${window.location.hostname}:8080` : "http://localhost:8080")).replace(/^http/, "ws");
    
    const client = new Client({
      brokerURL: `${wsUrl}/api/v1/central/ws`,
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    client.onConnect = () => {
      client.subscribe("/topic/admin/analytics", (message) => {
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
    return () => {
      client.deactivate();
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
    <div className="space-y-6">
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent">Overview</h1>
          <p className="text-slate-400 mt-1">Real-time statistics across the StreamSphere network</p>
        </div>
        <div className="flex items-center gap-2 px-3 py-1.5 bg-emerald-500/10 text-emerald-400 rounded-full text-sm border border-emerald-500/20 shadow-[0_0_15px_rgba(16,185,129,0.1)]">
          <div className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
          Live
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {metrics.map((m) => (
          <div key={m.label} className="relative overflow-hidden bg-slate-800/50 backdrop-blur-xl border border-slate-700/50 rounded-2xl p-6 transition-transform hover:-translate-y-1 hover:shadow-xl hover:shadow-indigo-500/10">
            <div className="flex items-center gap-4 relative z-10">
              <div className={`p-3 rounded-xl ${m.bg}`}>
                <m.icon className={`w-6 h-6 ${m.color}`} />
              </div>
              <div>
                <div className="text-sm font-medium text-slate-400">{m.label}</div>
                <div className="text-3xl font-bold text-slate-100">{m.value?.toLocaleString() ?? 0}</div>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="bg-slate-800/50 backdrop-blur-xl border border-slate-700/50 rounded-2xl p-6 mt-8">
        <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
          <Activity className="w-5 h-5 text-indigo-400" /> Live Traffic Activity
        </h2>
        <div className="h-80 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={history}>
              <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
              <XAxis dataKey="time" stroke="#94a3b8" fontSize={12} />
              <YAxis yAxisId="left" stroke="#94a3b8" fontSize={12} />
              <YAxis yAxisId="right" orientation="right" stroke="#94a3b8" fontSize={12} />
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

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mt-6">
        <div className="bg-slate-800/50 backdrop-blur-xl border border-slate-700/50 rounded-2xl p-6">
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <Users className="w-5 h-5 text-blue-400" /> Platform Growth
          </h2>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={history}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="time" stroke="#94a3b8" fontSize={12} />
                <YAxis stroke="#94a3b8" fontSize={12} />
                <Tooltip 
                  contentStyle={{ backgroundColor: "#1e293b", borderColor: "#334155", borderRadius: "8px" }}
                />
                <Line type="stepAfter" dataKey="totalUsers" stroke="#3b82f6" strokeWidth={3} dot={false} name="Users" />
                <Line type="stepAfter" dataKey="totalChannels" stroke="#10b981" strokeWidth={3} dot={false} name="Channels" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="bg-slate-800/50 backdrop-blur-xl border border-slate-700/50 rounded-2xl p-6">
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <Video className="w-5 h-5 text-purple-400" /> Content Distribution
          </h2>
          <div className="flex items-center justify-center h-64 w-full text-slate-400">
            {/* Using a pseudo-bar chart representation out of the Live data since we don't have categories */}
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={history}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="time" stroke="#94a3b8" fontSize={12} />
                <YAxis stroke="#94a3b8" fontSize={12} />
                <Tooltip 
                  contentStyle={{ backgroundColor: "#1e293b", borderColor: "#334155", borderRadius: "8px" }}
                />
                <Line type="monotone" dataKey="totalVideos" stroke="#8b5cf6" strokeWidth={3} fill="#8b5cf6" dot={false} name="Videos Uploaded" />
              </LineChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
}
