# BureauCat Implementation Plan

> **For agentic workers:** Execute task-by-task. Mark each step `- [x]` immediately after completion. At session start: find first unchecked step, announce it, continue from there. Spec: `architecture.md` — follow its conventions strictly. Commit after every task (Conventional Commits, English).

**Goal:** Dokument-Assistent — upload German bureaucracy letter → LLM analysis → structured card in Postgres → Drive/Notion/Calendar.

**Tech:** Java 21, Spring Boot 3.x, Gradle, PostgreSQL 16, Flyway, React 18 + TS + Vite + Tailwind + TanStack Query, Anthropic API.

**Token-saving rules for executor:**
- Don't re-read whole architecture.md each session — read only relevant phase section.
- Don't re-read files just edited.
- Mark steps done in batches per task (one Edit call per task completion).

---

## Phase 0 — Skeleton

### Task 0.1: Spring Boot app with health endpoint
**Files:** `backend/build.gradle`, `backend/src/main/resources/application.yml`
- [x] Convert build.gradle to Spring Boot 3.x (plugins: `org.springframework.boot`, `io.spring.dependency-management`; deps: web, actuator, data-jpa, validation, postgresql runtime, flyway-core + flyway-database-postgresql, testcontainers junit-jupiter + postgresql for tests)
- [x] application.yml: datasource from env vars (`DB_URL`, `DB_USER`, `DB_PASSWORD` with localhost defaults), JPA `ddl-auto: validate`, actuator health exposed
- [x] Verify: `.\gradlew.bat build -x test` compiles

### Task 0.2: Postgres in docker-compose
**Files:** `docker-compose.yml` (root)
- [x] compose: postgres:16 service, env vars, volume, port 5432, healthcheck; app service commented out until Dockerfile exists
- [x] Verify: `docker compose up -d postgres` → container healthy

### Task 0.3: Flyway first migration
**Files:** `backend/src/main/resources/db/migration/V1__create_document_card.sql`
- [x] V1 migration: `document_card` table per architecture.md §3 (enums as text + CHECK constraints, simpler with JPA)
- [x] Integration test: Testcontainers Postgres, context loads, Flyway applies (test class `BureauCatApplicationIT`)
- [x] Verify: `.\gradlew.bat test` green (required Testcontainers 1.21.4 bump for Docker Engine 29 API ≥1.44)
- [x] Commit `feat: spring boot app with postgres and flyway baseline`

### Task 0.4: Frontend proxy + cleanup
**Files:** `frontend/vite.config.ts`, `frontend/src/App.tsx`
- [x] vite.config.ts: proxy `/api` → `http://localhost:8080`
- [x] Strip Vite demo content from App.tsx; minimal page calling `GET /api/health` (trivial backend `HealthController` returning `{"status":"ok"}` under `/api/health`)
- [x] Install Tailwind CSS v4 + TanStack Query now (avoid rework in Phase 2)
- [x] Verify: backend run + `npm run dev` → `/api/health` via Vite proxy returns `{"status":"ok"}`
- [x] Commit `feat: frontend proxy and health check page`

### Task 0.5: CI
**Files:** `.github/workflows/ci.yml`
- [x] GitHub Actions: backend `gradlew build` (ubuntu-latest, docker available for Testcontainers), frontend `npm ci && npm run build`
- [x] Commit `ci: build and test workflow`
- [x] Mark Phase 0 done in architecture.md §8

## Phase 1 — Analysis pipeline (core)

### Task 1.1: Domain model + persistence
**Files:** `backend/src/main/java/com/bureaucat/cards/` (`DocumentCard.java` entity, enums `SourceType`, `DocType`, `RequiredAction`, `Urgency`, `Confidence`, `CardStatus`, `DocumentCardRepository.java`)
- [x] JPA entity mapping V1 table; JSONB fields via `@JdbcTypeCode(SqlTypes.JSON)`; enums `@Enumerated(STRING)`
- [x] Repository test (Testcontainers): save + findById roundtrip incl. JSONB
- [x] Commit `feat: document card entity and repository`

### Task 1.2: Upload endpoint + source type detection
**Files:** `com/bureaucat/ingestion/` (`DocumentController.java`, `SourceTypeDetector.java`, `IngestionService.java`)
- [x] `POST /api/documents` multipart, accept PDF/JPG/PNG, reject others (400); max size config (`MAX_UPLOAD_SIZE`, default 25MB)
- [x] `SourceTypeDetector`: PDF text layer via PDFBox `PDFTextStripper` (threshold ~50 chars/page avg) → PDF_TEXT, else PDF_SCAN; image → IMAGE. Add PDFBox dep.
- [x] Unit tests for detector (fixtures generated in-test via PDFBox — no binaries in repo)
- [x] Commit `feat: document upload with source type detection`

### Task 1.3: DocumentAnalyzer interface + Anthropic impl
**Files:** `com/bureaucat/analysis/` (`DocumentAnalyzer.java`, `AnalysisResult.java` record + nested records, `AnthropicAnalyzer.java`, `AnalysisPrompt.java`; props `anthropic.api-key`, `anthropic.model`)
- [ ] `AnalysisResult` record mirroring card fields + `confidenceReasoning`; jackson + bean validation
- [ ] Prompt per architecture.md §4: strict JSON only, russian summary (configurable), null for unknown, evidence quotes mandatory, LOW confidence rules
- [ ] `AnthropicAnalyzer`: PDF_TEXT → text block; PDF_SCAN → render pages to images (PDFBox) → image blocks; IMAGE → image block
- [ ] Invalid JSON → one retry with error appended, then fail saving raw response
- [ ] Unit tests: parse valid mock response; invalid → retry → fail path (mock HTTP)
- [ ] Commit `feat: anthropic document analyzer`

### Task 1.4: Pipeline wiring + cost tracking
**Files:** `IngestionService` (orchestrate), `com/bureaucat/analysis/AnalysisCost.java` + repo, migration `V2__analysis_cost.sql`
- [ ] V2: `analysis_cost` (id, card_id FK, model, input_tokens, output_tokens, cost_usd, created_at)
- [ ] Pipeline: upload → detect → analyze → map to DocumentCard (status NEW) → save card + cost row
- [ ] `POST /api/documents` returns 201 + card JSON
- [ ] Integration test: multipart upload with mocked analyzer → card in DB with quotes
- [ ] Commit `feat: analysis pipeline with cost tracking`

### Task 1.5: Read endpoints
**Files:** `com/bureaucat/cards/CardController.java`, DTOs
- [ ] `GET /api/documents/{id}` (404 if missing), `GET /api/documents?page=&size=` sorted created_at desc
- [ ] Integration tests
- [ ] Commit `feat: card read endpoints`
- [ ] Manual smoke: real letter via curl with real API key → card + quotes in DB (**user does this**)
- [ ] Mark Phase 1 done in architecture.md §8, update README

## Phase 2 — Minimal UI

### Task 2.1: API client + routing skeleton
- [ ] TanStack Query setup, typed API client (`frontend/src/api/`), react-router, layout
- [ ] Commit
### Task 2.2: Upload page
- [ ] Drag-n-drop + `<input capture>` camera, upload progress, redirect to card on success
- [ ] Commit
### Task 2.3: Card screen
- [ ] Summary, action steps, deadline, amount, quotes, confidence indicator (prominent on LOW), status change control
- [ ] Commit
### Task 2.4: Documents list
- [ ] List with filters: type/status/deadline; pagination
- [ ] Commit
### Task 2.5: PWA manifest
- [ ] manifest + icons, installable on phone
- [ ] Manual smoke: photo from phone → card visible (**user does this**)
- [ ] Commit, mark Phase 2 done

## Phase 3 — Drive + Notion (detail when reached)

- [ ] 3.1 Google OAuth credentials setup (env), Drive client, upload to `/Dokumente/{year}/{category}/{date}_{sender}.pdf`, store file id+link
- [ ] 3.2 Notion internal integration: page in DB with card fields + Drive link; document Notion schema in README
- [ ] 3.3 Async export (Spring events / @Async), idempotency (skip if drive_file_id set), retry with resilience4j
- [ ] 3.4 Export status in UI; mark Phase 3 done

## Phase 4 — Calendar + deadlines (detail when reached)

- [ ] 4.1 Calendar event creation w/ reminders (7d, 2d, configurable), title format per spec
- [ ] 4.2 User confirmation flow before event creation
- [ ] 4.3 Upcoming deadlines dashboard on main page; mark Phase 4 done

## Phase 5 — Quality + batch (detail when reached)

- [ ] 5.1 Batch endpoint/command for folder processing
- [ ] 5.2 `analysis_feedback` table + error capture, prompt iteration
- [ ] 5.3 `POST /api/documents/{id}/questions` doc-context chat
- [ ] 5.4 Personal archive run, ≥90% accuracy; mark Phase 5 done

---

## Session log
<!-- One line per session: date, last completed step -->
- 2026-06-10: Plan created. Tasks 0.1–0.5 implemented; Docker-dependent verify steps blocked (Docker Desktop won't start headless — user: start Docker Desktop, then run `docker compose up -d postgres` and `backend\gradlew.bat -p backend test`). Next: unblock verifies, then Task 1.1.
