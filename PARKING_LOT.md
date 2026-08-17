# Pilot Scanner - Feature Parking Lot

This document tracks future feature ideas, enhancements, and the housekeeping backlog.

## Active Backlog

### High Priority — Next Up

#### Bulk Scanning Mode
**Type**: New Feature (Minor Version)
**Status**: READY FOR DEVELOPMENT
**Description**: Enhanced rapid scanning interface for high-volume operations
**Key Requirements**:
- Rapid succession scanning with minimal delays
- Visual queue/list of scanned items
- Batch validation before committing
- Easy error correction and item removal
- Progress indicators and scan counters
- Configurable scan delays and timeouts
**Benefits**: Process large batches efficiently, reduce manual interaction, minimize UI interactions during continuous scanning

#### Enhanced Offline Mode
**Type**: New Feature (Minor Version)
**Status**: Partial — local export already works 100% offline
**What's Left**: Add sync queue for cloud uploads when connectivity returns
**Implementation**: Background sync service, queue management, automatic retry with exponential backoff

#### Unit Test Coverage KPIs
**Type**: Quality Initiative (tooling + ongoing)
**Status**: IN PROGRESS — JaCoCo wired (v2.9.2), CI coverage gate live at 25% floor (v2.9.9), continuing against the next tranche of targets
**Description**: Drive unit-test coverage up to measurable, risk-tiered targets. Coverage is a proxy — the rule is *test behavior, not lines*; framework-bound code (Activities, ViewBinding, factories) is excluded from the report so the % stays honest.

**Tooling**: `./gradlew jacocoTestReport` → HTML at `app/build/reports/jacoco/jacocoTestReport/html/index.html`. **Enforced in CI** (v2.9.9): `jacocoTestCoverageVerification` runs as part of `check`/`build` and fails the build if line coverage drops below the **25% floor**. Raise the floor as coverage climbs.

**Latest (v2.9.11, debug unit tests)**: **34.2% line** overall.
> ⚠️ The initial v2.9.2 baseline (13.9%) was **understated** — JaCoCo was silently dropping all Robolectric-driven tests (`includeNoLocationClasses` was off). Fixed in v2.9.3; numbers below are the corrected picture.

Per-package line:
| Package | Line % | Lines |
|---|---|---|
| `(root)` | 0% | 0/8 |
| `config` | 0% | 0/6 |
| `inventory` | 10.3% | 8/78 |
| `export` | 14.9% | 134/899 |
| `util` | 19.3% | 123/638 |
| `data` | 27.3% | 88/322 |
| `ocr` | 30.6% | 274/895 |
| `livescan/hid` | 51.5% | 104/202 |
| `export/datasource` | 52.2% | 131/251 |
| `ui` | 66.1% | 181/274 |
| `kitbundle` | 70.5% | 347/492 |

> `kitbundle` reached 70.5%: the domain models (`KitBundleState`/`RequirementStatus`) and `KitBundleViewModel` (86.9%, v2.9.11) are covered; the remaining gap is the Android-coupled Activity/dialog classes.

**KPI targets**:
- **Short-term (next 1–2 versions)**: overall floor **25% (no regression)**; all originally-planned high-risk targets are covered (`DsnValidator`, `OcrConfidenceManager`, `ImagePreprocessor`, all five export data sources, Kit Bundle domain models). Next pure-logic targets from the testability survey (2026-08-17): `data/KitBundle` string-split helpers, `kitbundle/KitBundleViewModel` slot-mapping logic, `data/ScanHistoryManager` trim/ordering, `data/BaseRepository.sanitizeInput`.
- **Long-term (3–6 months)**: core business-logic packages (`ocr`, `kitbundle`, `export`, `data`) **85% line / 75% branch**; overall **75% line**; CI gate at the floor.
- **New code**: 80% on changed files (stops the gap from growing).

**Execution order (highest-risk first)**: ~~`DsnValidator`~~ ✅ (v2.9.2) → ~~`OcrConfidenceManager`~~ ✅ (v2.9.3) → ~~Kit Bundle logic~~ ✅ (v2.9.4) → ~~`ExportDataSource` implementations~~ ✅ (v2.9.5) → ~~`ImagePreprocessor`~~ ✅ (v2.9.6) → ~~`LogsDataSource`~~ ✅ (v2.9.7). **All originally-planned high-risk targets covered.**

**Next tranche (testability survey, 2026-08-17 — ranked by bug-hiding ROI)**:
1. ~~`data/KitBundle` — `generateKitId`/`extractCreationDate`/`extractBaseKitCode` string-split edge cases~~ ✅ (v2.9.10) — 0% → 90%; clean (already uses last-dash splits defensively).
2. ~~`kitbundle/KitBundleViewModel` — confidence routing, duplicate-DSN reassignment, and three parallel slot-mapping `when`-blocks~~ ✅ (v2.9.11) — 0% → 86.9%; clean (mapping blocks in sync, no Robolectric needed).
3. `data/ScanHistoryManager` — 50-item trim, add-at-front ordering, update/delete-by-id, per-activity key routing. Robolectric.
4. `data/BaseRepository.sanitizeInput` (+ location-aware filename composition) — security-relevant input sanitization. Robolectric via a small test subclass.
5. `inventory/InventoryRecord.create` + `ComponentType.fromString` — scan-mode/type mapping; cheap pure-logic wins.

**Skip (survey-confirmed low/no value)**: `config/*` (constant holders — 0% is cosmetic), both ViewModelFactories, all DialogFragments, Activities, `CameraManager`, `WindowInsetsHelper`.
**Notes**:
- `DsnValidator` coverage (v2.9.2) surfaced a latent regex crash; `OcrConfidenceManager` (v2.9.3) surfaced an out-of-range confidence bug plus a pinned dead-branch known issue (now fixed in v2.9.8, see below). Kit Bundle logic (v2.9.4), the export data sources (v2.9.5), `ImagePreprocessor` (v2.9.6), and `LogsDataSource` (v2.9.7) were clean — no defects. Bugs cluster in the parsing/scoring code, not the model/mapping/math logic.
- `ImagePreprocessor` (v2.9.6) only reached 23.4% line coverage: its bitmap-transforming methods use `Canvas`/`ColorMatrixColorFilter` (no-op under Robolectric) and its YUV→Bitmap + private pixel passes need a real camera frame. Only the two pure-ish public methods (`analyzeImageQuality`, `getAdaptiveParameters`) are unit-testable — that is the ceiling here, not a coverage gap to close.
- v2.9.5 added an injectable-repository seam to the four data sources (backward-compatible default) to make them unit-testable. v2.9.7 extended the same seam to `LogsDataSource` (injectable `LogManager`), bringing it to 94.4% — all five export data sources are now covered.
- ~~**Follow-up (tuning, not coverage)**: decide whether to fix the MEDIUM-strictness length/prefix gate in `calculatePatternMatchScore`~~ ✅ **fixed in v2.9.8** — reworked to reuse `DsnValidator.inferComponentType`, so CONTROLLER/GLASSES perfect matches now score 0.95 (was dead-branched to 0.75), reducing spurious manual-verification prompts.

### Medium Priority

#### Live Scan Mode — Wi-Fi Companion Fallback
**Type**: Contingency Feature (only if Bluetooth HID is incompatible with target devices)
**Status**: PAUSED — pending Galaxy A14 5G test result
**BT HID Compatibility Findings (2026-07-07)**:
- Moto G 5G 2024: FAILED — `BluetoothProfile.HID_DEVICE` service not present in OEM BT stack.
  Fast-fail detection (3-second watchdog) added in `HidKeyboardService`.
- Galaxy A14 5G: NOT YET TESTED (device currently on loan)
**Trigger**: Build Wi-Fi fallback if Galaxy A14 5G also fails, OR if no compatible phone is available within the deployment fleet.
**Reference**: See LIVE_SCAN_MODE_DESIGN.md for full BT HID approach and risk table

#### User Authentication
**Type**: New Feature (Minor Version)
**Description**: Integrate authentication to track individual users' checkouts
**Benefits**: Audit trail, personal accountability, integration potential with internal systems
**Considerations**: Authentication method (local vs. cloud), user management, privacy

#### Analytics Dashboard
**Type**: New Feature (Minor Version)
**Description**: Simple dashboard for monitoring kit checkout trends
**Features**: Total checkouts per day, popular kits tracking, user activity reports, basic trend analysis

### Lower Priority

#### Error Handling Improvements
**Type**: Enhancement (Patch Version)
**Description**: Enhanced error messaging with specific guidance and retry logic for scan failures

#### Hardware Compatibility Optimization
**Type**: Enhancement (Patch Version)
**Description**: Optimize for different device cameras and form factors

#### Voice Confirmation
**Type**: New Feature (Patch Version)
**Description**: Optional voice feedback for successful scans (accessibility improvement)

#### Cross-Platform Development
**Type**: Major Change (Major Version)
**Description**: Evaluate moving to Flutter or React Native for iOS + tablet support
**Considerations**: Migration effort, performance impact, code reuse potential

## Housekeeping Backlog

### Build System
| Item | Type | Notes |
|------|------|-------|
| `copyApksToSharedLocation` task uses deprecated `project.buildDir` | Build | Still functional but should migrate to `layout.buildDirectory` |

### Code Quality
| Item | Type | Notes |
|------|------|-------|
| ~~9 failing unit tests (per v2.7.5 notes)~~ | Testing | RESOLVED — full suite green as of Aug 2026 |
| ~~No tests for OCR pipeline or Kit Bundle logic~~ | Testing | RESOLVED — `DsnValidator` (v2.9.2), `OcrConfidenceManager` (v2.9.3), Kit Bundle domain models (v2.9.4), `ImagePreprocessor` (v2.9.6, unit-testable surface) covered. Remaining ViewModel-level logic tracked in the coverage "next tranche" above. |
| ~~No tests for export data sources~~ | Testing | RESOLVED — all five `ExportDataSource` implementations covered (v2.9.5, v2.9.7) |
| Verify AWS Cognito dependency usage | Deps | S3 uses Access Key auth — Cognito SDK may be unused dead weight (~3MB) |

### Feature Gaps
| Item | Type | Notes |
|------|------|-------|
| `ScanHistoryManager` UI integration pending | Feature gap | Infrastructure built Dec 2025, wired in MainActivity but no dedicated UI |
| Live Scan BT HID — paused pending Galaxy A14 test | Feature gap | Phase 1 committed; blocked on device procurement |

## Completed Features (Reference)

| Feature | Version | Summary |
|---------|---------|---------|
| File Export System | v1.5.0–2.9.0 | JSON, CSV, XML, TXT, Kit Labels CSV; local/share/S3 destinations |
| Barcode Support | v1.0+ | QR, Code 128, Code 39, Code 93, UPC-A/E, EAN-13/8 |
| S3 Integration | v1.6.0–2.9.0 | Full S3 config UI, batch upload, retry with backoff, metadata tagging |
| OCR System | v2.1.0+ | Image preprocessing, confidence scoring, environmental adaptation |
| Inventory Management | v2.6.0 | Bulk device scanning up to 500 items |
| Material Design 3 | v2.8.0 | Full light/dark theme support |
| Unified Export System | v2.7.0 | UniversalExportManager, ExportDataSource interface |
| Toast Centralization | v2.9.1 | All user messages through DialogUtils |
| Unused Import Cleanup | v2.9.1 | Across all activities |
| Theme Fixes | v2.9.1 | Hardcoded colors replaced with theme-aware attributes |

## Offline Export Strategies

### Primary (No Connectivity Required)
1. **Local File Generation** — CSV/JSON to Downloads folder, immediate access via Files app
2. **Android Share Intent** — Bluetooth transfer, save to cloud apps when WiFi available later
3. **USB File Transfer** — Connect to laptop, access Downloads directly

### Secondary (When Connectivity Available)
4. **Automatic Cloud Sync** — Queue exports for upload when connectivity returns
5. **WiFi Hotspot** — Vehicle/base station WiFi for instant cloud uploads

## Notes

- All features maintain backward compatibility with existing JSON file format
- Export functionality must work without cellular/WiFi
- Security considerations needed for any authentication implementation
- Performance testing required for bulk operations
- User feedback should drive priority adjustments

---
*Last Updated: August 17, 2026*
