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
**Status**: READY FOR DEVELOPMENT — no coverage tooling wired yet
**Description**: Define and enforce measurable unit-test coverage targets, then drive coverage up to meet them.
**Plan**:
1. **Define** — Wire up JaCoCo (Gradle) to produce per-module coverage reports. Agree on KPI targets, e.g.:
   - Overall line coverage floor (suggest starting at current baseline, ratchet upward)
   - Higher bar for core business logic packages (`ocr`, `kitbundle`, `export`, `data`) — suggest 80%+
   - Optional CI gate: fail the build if coverage drops below the floor
2. **Measure** — Capture the current baseline once JaCoCo is in place; record it here as the starting KPI.
3. **Execute** — Close the highest-risk gaps first (see Code Quality backlog): `OcrConfidenceManager`, `ImagePreprocessor`, Kit Bundle logic, `ExportDataSource` implementations.
**Notes**: `DsnValidator` coverage (v2.9.2) is the first increment — it surfaced a latent regex crash, validating the ROI of this initiative.

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
