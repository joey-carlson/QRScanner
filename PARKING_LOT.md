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
**Status**: IN PROGRESS — JaCoCo wired (v2.9.2), baseline captured, executing against high-risk packages
**Description**: Drive unit-test coverage up to measurable, risk-tiered targets. Coverage is a proxy — the rule is *test behavior, not lines*; framework-bound code (Activities, ViewBinding, factories) is excluded from the report so the % stays honest.

**Tooling**: `./gradlew jacocoTestReport` → HTML at `app/build/reports/jacoco/jacocoTestReport/html/index.html`. Enforcement is **report-only** (no CI gate yet — add once comfortably above the floor).

**Baseline (v2.9.2, debug unit tests)**: **13.9% line · 10.0% branch** overall. Per-package line:
| Package | Line % | Lines |
|---|---|---|
| `export/datasource` | 0% | 0/246 |
| `kitbundle` | 0% | 0/492 |
| `inventory` | 0% | 0/78 |
| `data` | 3.1% | 10/322 |
| `export` | 10.3% | 92/897 |
| `util` | 10.5% | 67/638 |
| `ocr` | 12.4% | 111/897 |
| `livescan/hid` | 51.5% | 104/202 |
| `ui` | 66.1% | 181/274 |

**KPI targets**:
- **Short-term (next 1–2 versions)**: overall floor **14% (no regression)**; get the zero-coverage core packages off zero — `kitbundle` and `export/datasource` to ~50% (pure logic, high ROI). Roughly doubles overall coverage.
- **Long-term (3–6 months)**: core business-logic packages (`ocr`, `kitbundle`, `export`, `data`) **85% line / 75% branch**; overall **75% line**; add CI gate at the floor.
- **New code**: 80% on changed files (stops the gap from growing).

**Execution order (highest-risk first)**: ~~`DsnValidator`~~ ✅ (v2.9.2) → `OcrConfidenceManager` → Kit Bundle logic → `ExportDataSource` implementations → `ImagePreprocessor`.
**Notes**: `DsnValidator` coverage (v2.9.2) was the first increment — it surfaced a latent regex crash, validating the ROI of this initiative.

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
| ~~9 failing unit tests (per v2.7.5 notes)~~ | Testing | RESOLVED — full suite green (338 tests, 0 failures) as of Aug 2026 |
| No tests for OCR pipeline or Kit Bundle logic | Testing | Core business logic — partial: `DsnValidator` now covered (v2.9.2, 43 tests + regex crash fix). Remaining: `OcrConfidenceManager`, `ImagePreprocessor`, Kit Bundle logic |
| No tests for export data sources | Testing | `ExportDataSource` implementations untested |
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
*Last Updated: August 3, 2026*
