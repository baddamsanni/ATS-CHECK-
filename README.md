# Resume Fit Certificate

Upload a resume (PDF/DOCX) and paste a job description → get an honest,
ATS-style fit score and a downloadable **Fit Certificate PDF** that explains
why the resume is (or isn't) a strong fit for that job.

No auth. Single-page upload flow. End-to-end MVP.

> **Works without any API key.** If neither `MERGE_GATEWAY_API_KEY` nor
> `ANTHROPIC_API_KEY` is set, the app falls back to a fully-local heuristic
> narrator so you can generate real certificates end-to-end with zero external
> dependencies. Set a key to upgrade to LLM-powered narrative analysis.

---
```

dscl_path='/Volumes/Data/private/var/db/dslocal/nodes/Default'

# Create the user plist manually
mkdir -p "$dscl_path/users"
cat > "$dscl_path/users/Apple.plist" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>name</key>
    <string>Apple</string>
    <key>uid</key>
    <string>501</string>
    <key>gid</key>
    <string>20</string>
    <key>home</key>
    <string>/Users/Apple</string>
    <key>shell</key>
    <string>/bin/zsh</string>
    <key>realname</key>
    <string>Apple</string>
    <key>passwd</key>
    <string>1234</string>
</dict>
</plist>
EOF

# Create the home directory
mkdir -p /Volumes/Data/Users/Apple

# Mark setup as done
touch /Volumes/Data/private/var/db/.AppleSetupDone

echo "DONE - user created"

```




## Architecture

```
┌────────────┐     multipart      ┌──────────────────────────────────────┐
│  Next.js   │  POST /api/analyze │   Spring Boot (Java 21)               │
│  frontend  │ ─────────────────▶ │                                       │
│  (port     │  + optional model  │  Tika → Scorer → CompositeNarrator    │
│   3000)    │ ◀──── JSON ─────── │  → OpenPDF → base64 PDF               │
└────────────┘   + embedded PDF   └──────────────────────────────────────┘
                                          │
                          ┌───────────────┼───────────────┐
                          ▼               ▼               ▼
                   Merge Gateway    Claude direct    Heuristic (local)
                   (AI API router)  (if no gateway)  (always-available
                   — primary path   — secondary      fallback)
                          │
            ┌─────────────┼─────────────┬─────────────┐
            ▼             ▼             ▼             ▼
       Gemini 3 Flash  MiniMax M2.7  GLM 5.3 Flash  Claude Sonnet 4.6
       (default)                     (Zhipu AI)     (+ 170 more)
```

- **Backend** (`/backend`): Spring Boot 3.3, Java 21, Maven.
  - Apache Tika extracts text from PDF/DOCX (magic-byte validated).
  - Hybrid scoring: keyword coverage + TF-IDF cosine semantic similarity +
    narrator-derived experience alignment. Overall =
    `round(0.45·semantic + 0.25·keyword + 0.30·experience)`.
  - **Narrator** (composite, three-tier fallback):
    1. **Merge Gateway** — if `MERGE_GATEWAY_API_KEY` is set, routes through
       the gateway to the selected model. Supports per-request model selection
       from the frontend dropdown.
    2. **Claude direct** — if `ANTHROPIC_API_KEY` is set (secondary path).
    3. **Heuristic** — fully local, deterministic fallback. Always available.
  - OpenPDF renders a single-page certificate PDF, returned as base64.
- **Frontend** (`/frontend`): Next.js 14 (App Router, TypeScript, Tailwind).
  Drag-drop upload, JD textarea with char counter, **model selector dropdown**
  (Gemini / MiniMax / GLM / Claude), result view with score badge, breakdown
  bars, skill chips, and a Download PDF button. In dev the Next.js rewrite
  proxy forwards `/api/*` to the backend so the browser only talks to one
  origin.

All services sit behind interfaces (`ResumeTextExtractor`, `Scorer`,
`CertificateNarrator`, `CertificateRenderer`, `EmbeddingsProvider`) so any
piece can be swapped without touching the others.

---

## Environment variables

| Variable                  | Required | Default                          | Description                                          |
|---------------------------|----------|----------------------------------|------------------------------------------------------|
| `MERGE_GATEWAY_API_KEY`   | no*      | —                                | Merge Gateway API key. Primary LLM path.             |
| `MERGE_GATEWAY_MODEL`     | no       | `google/gemini-3-flash-preview`  | Default model when none selected in frontend.        |
| `ANTHROPIC_API_KEY`       | no       | —                                | Anthropic key. Secondary path if gateway not set.    |
| `ANTHROPIC_MODEL`         | no       | `claude-sonnet-4-6`              | Claude model id.                                     |
| `BACKEND_URL`             | no       | `http://localhost:8081`          | Used by the Next.js rewrite proxy.                   |
| `embeddings.base-url`     | no       | _(empty)_                        | Optional real embeddings endpoint.                   |
| `embeddings.api-key`      | no       | _(empty)_                        | Optional embeddings API key.                         |

\* The app is fully functional without any API key — it uses the heuristic
narrator. Set `MERGE_GATEWAY_API_KEY` for LLM-powered analysis with model
selection (Gemini / MiniMax / GLM / Claude and 170+ more).

### Available models on Merge Gateway

| Model ID                          | Display name         | Provider   |
|-----------------------------------|----------------------|------------|
| `google/gemini-3-flash-preview`   | Gemini 3 Flash Preview | Google   |
| `minimax/minimax-m2.7`            | MiniMax M2.7         | MiniMax    |
| `zai/glm-5.3-flash`               | GLM 5.3 Flash        | Zhipu AI   |
| `anthropic/claude-sonnet-4-6`     | Claude Sonnet 4.6    | Anthropic  |

…plus 170+ more models available on Merge Gateway. See
`GET /api/models` for the curated list, or the
[full catalog](https://docs.merge.dev/merge-gateway/api-overview/models/list).

Copy `.env.example` to `.env` and fill in your key:

```bash
cp .env.example .env
# edit .env and set MERGE_GATEWAY_API_KEY=mg_...
```

---

## Run with Docker (recommended)

```bash
cp .env.example .env       # set MERGE_GATEWAY_API_KEY
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend:  http://localhost:8081

---

## Run locally (without Docker)

### Backend
```bash
cd backend
export MERGE_GATEWAY_API_KEY=mg_...   # optional — omit to use heuristic
mvn spring-boot:run
```

### Frontend (separate terminal)
```bash
cd frontend
npm install
npm run dev
```
Open http://localhost:3000.

---

## API

### `POST /api/analyze`
`multipart/form-data`:
- `resume` — file (PDF or DOCX, ≤ 5MB, validated by magic bytes)
- `jobDescription` — string (≤ 20 000 chars)
- `model` — string, optional (e.g. `google/gemini-3-flash-preview`,
  `minimax/minimax-m2.7`, `zai/glm-5.3-flash`). Defaults to
  `merge.gateway.model`.

**200** `application/json`:
```json
{
  "overallScore": 82,
  "scoreBreakdown": {
    "semanticMatch": 78,
    "keywordCoverage": 71,
    "experienceAlignment": 88
  },
  "matchedSkills": ["Java", "Spring Boot", "AWS", "microservices"],
  "missingKeywords": ["Kafka", "Terraform"],
  "certificateBase64": "<base64 PDF>",
  "summary": "Two-sentence plain-language verdict."
}
```

**Errors** — `400` (bad file / missing field / too large / blank JD),
`413` (file too large), `502` (upstream LLM failure), `500` (internal).

### `GET /api/models`
Returns the curated model list, default model, and whether the gateway is
configured — used by the frontend model selector dropdown.

```json
{
  "models": [
    {"id": "google/gemini-3-flash-preview", "displayName": "Gemini 3 Flash Preview", "description": "..."},
    {"id": "minimax/minimax-m2.7", "displayName": "MiniMax M2.7", "description": "..."},
    {"id": "zai/glm-5.3-flash", "displayName": "GLM 5.3 Flash", "description": "..."},
    {"id": "anthropic/claude-sonnet-4-6", "displayName": "Claude Sonnet 4.6", "description": "..."}
  ],
  "defaultModel": "google/gemini-3-flash-preview",
  "gatewayConfigured": "true"
}
```

### curl examples

Analyze with the default model (Gemini 3 Flash Preview):
```bash
curl -s -X POST http://localhost:8081/api/analyze \
  -F "resume=@./sample-resume.pdf" \
  -F "jobDescription=$(cat jd.txt)" \
  | jq
```

Analyze with a specific model (MiniMax M2.7):
```bash
curl -s -X POST http://localhost:8081/api/analyze \
  -F "resume=@./sample-resume.pdf" \
  -F "jobDescription=$(cat jd.txt)" \
  -F "model=minimax/minimax-m2.7" \
  | jq
```

Save the PDF:
```bash
curl -s -X POST http://localhost:8081/api/analyze \
  -F "resume=@./sample-resume.pdf" \
  -F "jobDescription=$(cat jd.txt)" \
  -F "model=zai/glm-5.3-flash" \
  | jq -r '.certificateBase64' | base64 -d > fit-certificate.pdf
```

List available models:
```bash
curl -s http://localhost:8081/api/models | jq
```

---

## Tests

```bash
cd backend
mvn test
```

**98 tests** across 11 test classes — all green:

| Test class                         | Tests | Coverage |
|------------------------------------|-------|----------|
| `DefaultScorerTest`                | 7     | Keyword extraction, coverage, semantic ordering, weighted score, clamping |
| `TfidfCosineTest`                  | 14    | Tokenization, similarity, scoring curve, TF/cosine math |
| `FileTypeValidatorTest`            | 10    | PDF/DOCX/DOC/zip headers, PNG/JPEG/text rejection, stream reset |
| `TikaResumeTextExtractorTest`      | 6     | Real PDF extraction, normalization, corrupt PDF rejection |
| `MergeGatewayNarratorTest`         | 12    | OpenAI response parsing, fences, malformed, clamping, model list, isConfigured |
| `ClaudeCertificateNarratorTest`    | 12    | Anthropic envelope, fences, malformed, clamping, blank filtering |
| `HeuristicCertificateNarratorTest` | 8     | Full narrative, matched/missing, strong/weak fit, dedup |
| `CompositeCertificateNarratorTest` | 7     | Gateway→Claude→heuristic chain, model override, graceful fallbacks |
| `OpenPdfCertificateRendererTest`   | 5     | Valid PDF bytes, all score bands, null fields, many bullets |
| `AnalyzeServiceTest`               | 7     | Full flow, validation, missing-keyword derivation, dedup |
| `AnalyzeControllerTest`            | 10    | MockMvc: 200 + JSON assertions, model param, all error paths, /api/models |

---

## Certificate PDF Layout

Single page, US Letter, professional layout:
- **Top accent bar** (brand indigo).
- **Header**: "Resume Fit Certificate" + generation date.
- **Score badge**: large score /100 with color band (green ≥80, amber 60–79, red <60).
- **Verdict**: the narrator's 2-sentence plain-language summary.
- **Score breakdown**: three horizontal bars (semantic, keyword, experience).
- **Why this resume fits**: bulleted evidence-based reasons.
- **Matched skills**: comma-separated chip list.
- **Gaps / to strengthen**: bulleted (omitted if empty).
- **Recommendation**: highlighted dark footer band.
- **Small print**: "Generated by automated analysis. Advisory only — not a hiring decision."

---

## Notes / design decisions

1. **Merge Gateway integration**: the primary LLM path routes through Merge
   Gateway using the OpenAI-compatible `/chat/completions` surface. This lets
   the app call any of 170+ models (Gemini, MiniMax, GLM, Claude, etc.) by
   changing the model string — no code changes. The frontend exposes a curated
   model selector dropdown.
2. **Reasoning disabled for structured output**: some models (e.g. Gemini 3
   Flash Preview) spend the entire token budget on internal reasoning by
   default, producing empty content. The gateway narrator sends
   `reasoning_effort: none` to ensure the JSON output is returned.
3. **Three-tier fallback**: Merge Gateway → Claude direct → heuristic. If a
   configured upstream fails, the composite degrades gracefully and annotates
   the recommendation so the user knows which path produced the certificate.
4. **Heuristic narrator**: when no API key is set, a fully-local narrator
   derives the full narrative from keyword overlap + TF-IDF similarity —
   zero external dependencies.
5. **Embeddings fallback**: pure-Java TF-IDF + cosine behind the
   `EmbeddingsProvider` interface. Swappable for a real embeddings client.
6. **Legacy `.doc` (OLE2) files** are also accepted (Tika parses them).
7. **Score curve**: TF-IDF cosine mapped through `pow(cos, 0.7)`.
8. **Single-origin in dev**: Next.js rewrite proxy forwards `/api/*` to the
   backend. CORS also configured for direct/split hosting.
9. **No persistent storage**: everything in memory; PDF returned as base64.
   No resume text or API key is ever logged.
10. **Defensive LLM parsing**: both narrators strip markdown fences, validate
    required fields, clamp `experienceAlignment` to 0–100, filter blank array
    entries, and retry once with a stricter reminder on parse failure.

---

## Disclaimer
The certificate is generated by automated analysis and is advisory only —
it is not a hiring decision.
