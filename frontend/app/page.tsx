"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useDropzone } from "react-dropzone";
import type { AnalyzeResponse, ModelOption } from "./types";
import TierBadge from "../components/TierBadge";
import RequirementTable from "../components/RequirementTable";

const MAX_FILE_BYTES = 5 * 1024 * 1024;
const MAX_JD_CHARS = 20000;
const ACCEPTED = {
  "application/pdf": [".pdf"],
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document": [".docx"],
  "application/msword": [".doc"],
};

export default function Page() {
  const [file, setFile] = useState<File | null>(null);
  const [jd, setJd] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<AnalyzeResponse | null>(null);
  const [dragError, setDragError] = useState<string | null>(null);
  const [models, setModels] = useState<ModelOption[]>([]);
  const [selectedModel, setSelectedModel] = useState<string>("");
  const [gatewayConfigured, setGatewayConfigured] = useState(false);
  const resultRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    fetch("/api/models")
      .then((r) => r.json())
      .then((d) => {
        setModels(d.models || []);
        setGatewayConfigured(d.gatewayConfigured === "true");
        setSelectedModel(d.defaultModel || "");
      })
      .catch(() => {});
  }, []);

  const onDrop = useCallback((accepted: File[], rejections: any[]) => {
    setDragError(null);
    if (rejections && rejections.length > 0) {
      setDragError(rejections[0].file?.size > MAX_FILE_BYTES ? "File exceeds the 5MB limit." : "Only PDF and DOCX files are accepted.");
      return;
    }
    if (accepted.length > 0) setFile(accepted[0]);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop, accept: ACCEPTED, maxSize: MAX_FILE_BYTES, multiple: false,
  });

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);
    if (!file) { setError("Please upload a resume."); return; }
    if (!jd.trim()) { setError("Please paste a job description."); return; }
    if (jd.length > MAX_JD_CHARS) { setError(`Job description is too long (max ${MAX_JD_CHARS} chars).`); return; }
    setLoading(true);
    try {
      const fd = new FormData();
      fd.append("resume", file);
      fd.append("jobDescription", jd.trim());
      if (selectedModel) fd.append("model", selectedModel);
      const res = await fetch("/api/analyze", { method: "POST", body: fd });
      const data = await res.json();
      if (!res.ok) throw new Error(data?.error || `Request failed (${res.status}).`);
      setResult(data as AnalyzeResponse);
      setTimeout(() => resultRef.current?.scrollIntoView({ behavior: "smooth", block: "start" }), 50);
    } catch (err: any) {
      setError(err?.message || "Something went wrong.");
    } finally {
      setLoading(false);
    }
  }

  function handleDownload() {
    if (!result?.certificateBase64) return;
    const binary = atob(result.certificateBase64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
    const blob = new Blob([bytes], { type: "application/pdf" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url; a.download = "fit-certificate.pdf";
    document.body.appendChild(a); a.click(); a.remove();
    URL.revokeObjectURL(url);
  }

  function handleReset() { setFile(null); setJd(""); setError(null); setResult(null); }

  return (
    <main className="min-h-screen" style={{ background: "#FFF9F4", color: "#1C1917" }}>
      <div className="mx-auto max-w-3xl px-5 py-10">
        <header className="mb-8 text-center">
          <h1 className="font-serif text-3xl font-bold tracking-tight" style={{ color: "#1C1917" }}>
            RESUME CERTIFIED&gt;
          </h1>
          <p className="mt-1 text-sm" style={{ color: "#78716C" }}>
            Upload a resume, paste a job description, get a premium fit certificate.
          </p>
        </header>

        <form onSubmit={handleSubmit} className="rounded-2xl border bg-white p-6 shadow-sm sm:p-8" style={{ borderColor: "#F5E6D8" }}>
          <label className="mb-2 block text-sm font-semibold">Resume (PDF or DOCX, max 5MB)</label>
          <div
            {...getRootProps()}
            className="flex cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-10 text-center transition"
            style={{
              borderColor: isDragActive ? "#EA580C" : "#F5E6D8",
              background: isDragActive ? "#FFEDD5" : "#FFFBF7",
            }}
          >
            <input {...getInputProps()} />
            {file ? (
              <div>
                <p className="font-medium">{file.name}</p>
                <p className="mt-1 text-xs" style={{ color: "#A8A29E" }}>{(file.size / 1024).toFixed(1)} KB</p>
              </div>
            ) : isDragActive ? (
              <p style={{ color: "#EA580C" }}>Drop here…</p>
            ) : (
              <p style={{ color: "#A8A29E" }}>Drag &amp; drop, or click to browse</p>
            )}
          </div>
          {dragError && <p className="mt-2 text-xs" style={{ color: "#DC2626" }}>{dragError}</p>}

          <div className="mt-5">
            <div className="mb-2 flex items-center justify-between">
              <label htmlFor="jd" className="text-sm font-semibold">Job description</label>
              <span className="text-xs" style={{ color: jd.length > MAX_JD_CHARS ? "#DC2626" : "#A8A29E" }}>
                {jd.length.toLocaleString()} / {MAX_JD_CHARS.toLocaleString()}
              </span>
            </div>
            <textarea
              id="jd" value={jd} onChange={(e) => setJd(e.target.value)}
              maxLength={MAX_JD_CHARS}
              placeholder="Paste the job description..."
              className="h-36 w-full resize-y rounded-xl border p-3 text-sm outline-none"
              style={{ borderColor: "#F5E6D8" }}
            />
          </div>

          {models.length > 0 && (
            <div className="mt-5">
              <label htmlFor="model" className="mb-2 block text-sm font-semibold">
                Model {!gatewayConfigured && <span className="font-normal" style={{ color: "#A8A29E" }}>(heuristic fallback)</span>}
              </label>
              <select
                id="model" value={selectedModel} onChange={(e) => setSelectedModel(e.target.value)}
                disabled={!gatewayConfigured}
                className="w-full rounded-xl border p-3 text-sm outline-none disabled:cursor-not-allowed"
                style={{ borderColor: "#F5E6D8" }}
              >
                {models.map((m) => (
                  <option key={m.id} value={m.id}>{m.displayName}</option>
                ))}
              </select>
            </div>
          )}

          {error && (
            <div className="mt-4 rounded-lg border px-4 py-3 text-sm" style={{ borderColor: "#FECACA", background: "#FEF2F2", color: "#DC2626" }}>
              {error}
            </div>
          )}

          <div className="mt-5 flex flex-col gap-3 sm:flex-row">
            <button
              type="submit" disabled={loading}
              className="inline-flex flex-1 items-center justify-center rounded-xl px-5 py-3 font-semibold text-white shadow-sm transition disabled:opacity-60"
              style={{ background: "#EA580C" }}
            >
              {loading ? <><Spinner /> Analyzing…</> : "Generate Certificate"}
            </button>
            {result && (
              <button type="button" onClick={handleReset} className="rounded-xl border px-5 py-3 font-semibold" style={{ borderColor: "#F5E6D8" }}>
                Reset
              </button>
            )}
          </div>
        </form>

        {result && (
          <div ref={resultRef} className="mt-6 space-y-5">
            <div className="rounded-2xl border bg-white p-8 text-center shadow-sm" style={{ borderColor: "#F5E6D8" }}>
              <TierBadge tier={result.tier} />
              <p className="mt-2 text-sm" style={{ color: "#78716C" }}>{result.candidateName} · {result.roleTitle}</p>
            </div>

            <div className="rounded-2xl border bg-white p-5 shadow-sm" style={{ borderColor: "#F5E6D8" }}>
              <p className="text-sm leading-relaxed" style={{ color: "#44403C" }}>{result.summary}</p>
            </div>

            <div>
              <div className="mb-2 text-xs font-bold uppercase tracking-widest" style={{ color: "#EA580C" }}>Requirement Match</div>
              <RequirementTable requirements={result.requirements} />
            </div>

            {result.matchedSkills && result.matchedSkills.length > 0 && (
              <div>
                <div className="mb-2 text-xs font-bold uppercase tracking-widest" style={{ color: "#EA580C" }}>Matched Skills</div>
                <div className="flex flex-wrap gap-1.5">
                  {result.matchedSkills.map((s, i) => (
                    <span key={i} className="rounded-lg px-3 py-1 text-xs font-medium" style={{ background: "#FFEDD5", color: "#C2410C" }}>{s}</span>
                  ))}
                </div>
              </div>
            )}

            <div className="flex flex-col gap-3 border-t pt-4 sm:flex-row sm:items-center sm:justify-between" style={{ borderColor: "#F5E6D8" }}>
              <button onClick={handleDownload} className="inline-flex items-center justify-center gap-2 rounded-xl px-5 py-3 font-semibold text-white shadow-sm" style={{ background: "#EA580C" }}>
                <DownloadIcon /> Download Certificate
              </button>
              <span className="font-mono text-xs" style={{ color: "#A8A29E" }}>{result.verificationId}</span>
            </div>
          </div>
        )}
      </div>
    </main>
  );
}

function Spinner() {
  return (
    <svg className="mr-2 h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
    </svg>
  );
}

function DownloadIcon() {
  return (
    <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
      <path d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.293a1 1 0 011.414 0L9 11.586V3a1 1 0 112 0v8.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" />
    </svg>
  );
}
