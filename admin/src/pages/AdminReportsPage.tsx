import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ExternalLink, Flag, Search } from "lucide-react";
import { request } from "@/lib/adminApi";

type Report = {
  id: number;
  videoId: string;
  videoName: string;
  videoLink?: string;
  thumbnailUrl?: string;
  reporterName: string;
  reporterEmail: string;
  reason: string;
  description?: string | null;
  status: "PENDING" | "REVIEWED" | "RESOLVED" | "REJECTED";
  createdAt: string;
};

type ReportPage = {
  content: Report[];
  number: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
};

const pageSize = 10;
const statusOptions = ["ALL", "PENDING", "REVIEWED", "RESOLVED", "REJECTED"] as const;

function statusStyle(status: Report["status"]) {
  switch (status) {
    case "PENDING": return "border-amber-500/30 bg-amber-500/10 text-amber-300";
    case "REVIEWED": return "border-blue-500/30 bg-blue-500/10 text-blue-300";
    case "RESOLVED": return "border-emerald-500/30 bg-emerald-500/10 text-emerald-300";
    case "REJECTED": return "border-red-500/30 bg-red-500/10 text-red-300";
  }
}

function ReportActions({ report, onChange, pending }: {
  report: Report;
  onChange: (status: Report["status"]) => void;
  pending: boolean;
}) {
  const actions: Report["status"][] = report.status === "PENDING"
    ? ["REVIEWED", "RESOLVED", "REJECTED"]
    : report.status === "REVIEWED"
      ? ["RESOLVED", "REJECTED"]
      : ["PENDING"];

  return (
    <div className="flex flex-wrap gap-2">
      {actions.map((status) => (
        <button
          key={status}
          type="button"
          disabled={pending}
          onClick={() => onChange(status)}
          className="rounded-lg border border-slate-700 px-3 py-2 text-xs font-medium text-slate-200 transition hover:bg-slate-800 disabled:cursor-wait disabled:opacity-50"
        >
          {status === "PENDING" ? "Reopen" : `Mark ${status.toLowerCase()}`}
        </button>
      ))}
    </div>
  );
}

export default function AdminReportsPage() {
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState<(typeof statusOptions)[number]>("ALL");
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const queryClient = useQueryClient();
  const queryKey = ["admin-reports", page, status, query];
  const reportsQuery = useQuery<ReportPage>({
    queryKey,
    queryFn: () => {
      const params = new URLSearchParams({ page: String(page), size: String(pageSize) });
      if (status !== "ALL") params.set("status", status);
      if (query) params.set("search", query);
      return request(`/api/v1/admin/reports?${params.toString()}`);
    },
  });
  const updateStatus = useMutation({
    mutationFn: ({ id, nextStatus }: { id: number; nextStatus: Report["status"] }) =>
      request(`/api/v1/admin/reports/${id}/status?status=${nextStatus}`, { method: "PUT" }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["admin-reports"] }),
  });

  function submitSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setQuery(search.trim());
  }

  const reports = reportsQuery.data?.content || [];

  return (
    <div className="min-w-0 space-y-5 sm:space-y-6">
      <header className="flex min-w-0 flex-wrap items-center gap-3">
        <div className="rounded-xl bg-red-500/10 p-3 text-red-300"><Flag className="h-6 w-6" /></div>
        <div className="min-w-0 flex-1">
          <h1 className="text-2xl font-bold text-slate-100 sm:text-3xl">Reports</h1>
          <p className="mt-1 text-sm text-slate-400">Review and manage user-submitted video reports.</p>
        </div>
        {reportsQuery.data && (
          <span className="text-sm text-slate-400">{reportsQuery.data.totalElements.toLocaleString()} total</span>
        )}
      </header>

      <section className="min-w-0 rounded-2xl border border-slate-800/60 bg-slate-900/50 p-3 sm:p-5">
        <div className="flex flex-col gap-3 sm:flex-row">
          <form onSubmit={submitSearch} className="flex min-w-0 flex-1 gap-2">
            <label htmlFor="report-search" className="sr-only">Search reports</label>
            <div className="relative min-w-0 flex-1">
              <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
              <input
                id="report-search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search title, reason, or report ID"
                className="h-11 w-full min-w-0 rounded-lg border border-slate-700 bg-slate-950/60 pl-9 pr-3 text-sm text-slate-100 placeholder:text-slate-500 focus:border-indigo-400 focus:outline-none"
              />
            </div>
            <button type="submit" className="shrink-0 rounded-lg bg-indigo-600 px-4 text-sm font-medium text-white hover:bg-indigo-500">Search</button>
          </form>
          <label className="flex items-center gap-2 text-sm text-slate-400">
            <span className="sr-only">Filter report status</span>
            <select
              value={status}
              onChange={(event) => {
                setStatus(event.target.value as (typeof statusOptions)[number]);
                setPage(0);
              }}
              className="h-11 w-full rounded-lg border border-slate-700 bg-slate-950/60 px-3 text-sm text-slate-200 sm:w-auto"
            >
              {statusOptions.map((option) => <option key={option} value={option}>{option === "ALL" ? "All statuses" : option}</option>)}
            </select>
          </label>
        </div>

        {updateStatus.isError && (
          <p role="alert" className="mt-3 rounded-lg border border-red-500/20 bg-red-500/10 p-3 text-sm text-red-300">
            {updateStatus.error instanceof Error ? updateStatus.error.message : "Could not update report status."}
          </p>
        )}

        {reportsQuery.isLoading && <p className="py-12 text-center text-sm text-slate-400">Loading reports…</p>}
        {reportsQuery.isError && (
          <div role="alert" className="my-4 rounded-xl border border-red-500/20 bg-red-500/10 p-4 text-sm text-red-300">
            Could not load reports: {reportsQuery.error instanceof Error ? reportsQuery.error.message : "Unknown error"}
            <button type="button" onClick={() => void reportsQuery.refetch()} className="ml-3 underline">Retry</button>
          </div>
        )}
        {reportsQuery.isSuccess && reports.length === 0 && (
          <div className="py-12 text-center">
            <Flag className="mx-auto h-8 w-8 text-slate-600" />
            <p className="mt-3 font-medium text-slate-300">No reports found</p>
            <p className="mt-1 text-sm text-slate-500">
              {query || status !== "ALL" ? "Try changing the search or status filter." : "No reports have been submitted yet."}
            </p>
          </div>
        )}

        {reports.length > 0 && (
          <>
            <div className="mt-4 hidden overflow-x-auto lg:block">
              <table className="w-full min-w-[900px] table-fixed text-left text-sm text-slate-300">
                <thead className="border-b border-slate-700/70 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="w-[30%] px-3 py-3">Video</th>
                    <th className="w-[19%] px-3 py-3">Reporter</th>
                    <th className="w-[24%] px-3 py-3">Report</th>
                    <th className="w-[12%] px-3 py-3">Status</th>
                    <th className="w-[15%] px-3 py-3">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-800/70">
                  {reports.map((report) => (
                    <tr key={report.id} className="align-top hover:bg-slate-800/20">
                      <td className="px-3 py-4">
                        <div className="flex gap-3">
                          <img
                            src={report.thumbnailUrl || "/thumbnail-placeholder.svg"}
                            alt=""
                            className="aspect-video w-24 shrink-0 rounded-md bg-slate-800 object-cover"
                            onError={(event) => { event.currentTarget.src = "/thumbnail-placeholder.svg"; }}
                          />
                          <div className="min-w-0">
                            <p className="line-clamp-2 font-medium text-slate-100">{report.videoName}</p>
                            <p className="mt-1 break-all font-mono text-xs text-slate-500">{report.videoId}</p>
                            {report.videoLink && <a className="mt-1 inline-flex items-center gap-1 text-xs text-indigo-300 hover:underline" href={report.videoLink} target="_blank" rel="noreferrer">Open video <ExternalLink className="h-3 w-3" /></a>}
                          </div>
                        </div>
                      </td>
                      <td className="break-words px-3 py-4">
                        <p className="font-medium text-slate-200">{report.reporterName || "Unknown user"}</p>
                        <p className="mt-1 text-xs text-slate-500">{report.reporterEmail}</p>
                      </td>
                      <td className="break-words px-3 py-4">
                        <p className="font-medium text-slate-200">#{report.id} · {report.reason}</p>
                        {report.description && <p className="mt-1 line-clamp-3 text-xs text-slate-400">{report.description}</p>}
                        <time className="mt-2 block text-xs text-slate-500" dateTime={report.createdAt}>{new Date(report.createdAt).toLocaleString()}</time>
                      </td>
                      <td className="px-3 py-4"><span className={`inline-flex rounded-full border px-2.5 py-1 text-xs ${statusStyle(report.status)}`}>{report.status}</span></td>
                      <td className="px-3 py-4">
                        <ReportActions
                          report={report}
                          pending={updateStatus.isPending}
                          onChange={(nextStatus) => updateStatus.mutate({ id: report.id, nextStatus })}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="mt-4 space-y-3 lg:hidden">
              {reports.map((report) => (
                <article key={report.id} className="min-w-0 rounded-xl border border-slate-800 bg-slate-950/40 p-3 sm:p-4">
                  <div className="flex min-w-0 gap-3">
                    <img
                      src={report.thumbnailUrl || "/thumbnail-placeholder.svg"}
                      alt=""
                      className="aspect-video w-28 shrink-0 rounded-md bg-slate-800 object-cover sm:w-36"
                      onError={(event) => { event.currentTarget.src = "/thumbnail-placeholder.svg"; }}
                    />
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-start justify-between gap-2">
                        <p className="min-w-0 flex-1 break-words font-medium text-slate-100">{report.videoName}</p>
                        <span className={`rounded-full border px-2 py-1 text-[10px] ${statusStyle(report.status)}`}>{report.status}</span>
                      </div>
                      <p className="mt-1 break-all font-mono text-[11px] text-slate-500">{report.videoId}</p>
                      {report.videoLink && <a href={report.videoLink} target="_blank" rel="noreferrer" className="mt-1 inline-flex items-center gap-1 text-xs text-indigo-300">Open video <ExternalLink className="h-3 w-3" /></a>}
                    </div>
                  </div>
                  <details className="mt-3 border-t border-slate-800 pt-3">
                    <summary className="cursor-pointer text-sm font-medium text-slate-300">Report #{report.id} details</summary>
                    <dl className="mt-3 grid grid-cols-1 gap-2 text-sm sm:grid-cols-2">
                      <div><dt className="text-xs text-slate-500">Reporter</dt><dd className="break-words text-slate-200">{report.reporterName} · {report.reporterEmail}</dd></div>
                      <div><dt className="text-xs text-slate-500">Reason</dt><dd className="break-words text-slate-200">{report.reason}</dd></div>
                      {report.description && <div className="sm:col-span-2"><dt className="text-xs text-slate-500">Details</dt><dd className="break-words text-slate-300">{report.description}</dd></div>}
                      <div><dt className="text-xs text-slate-500">Submitted</dt><dd className="text-slate-300">{new Date(report.createdAt).toLocaleString()}</dd></div>
                    </dl>
                  </details>
                  <div className="mt-3"><ReportActions report={report} pending={updateStatus.isPending} onChange={(nextStatus) => updateStatus.mutate({ id: report.id, nextStatus })} /></div>
                </article>
              ))}
            </div>
            <footer className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-slate-800/70 pt-4">
              <span className="text-xs text-slate-500">
                {reportsQuery.data?.totalElements.toLocaleString() || 0} reports · page {(reportsQuery.data?.number ?? 0) + 1} of {Math.max(reportsQuery.data?.totalPages ?? 0, 1)}
              </span>
              <div className="flex gap-2">
                <button
                  type="button"
                  disabled={!reportsQuery.data || reportsQuery.data.first || reportsQuery.isFetching}
                  onClick={() => setPage((current) => Math.max(0, current - 1))}
                  className="rounded-lg bg-slate-800 px-3 py-2 text-sm text-slate-200 disabled:opacity-40"
                >Previous</button>
                <button
                  type="button"
                  disabled={!reportsQuery.data || reportsQuery.data.last || reportsQuery.isFetching}
                  onClick={() => setPage((current) => current + 1)}
                  className="rounded-lg bg-slate-800 px-3 py-2 text-sm text-slate-200 disabled:opacity-40"
                >Next</button>
              </div>
            </footer>
          </>
        )}
      </section>
    </div>
  );
}
