# Pilot Scanner - Feature Parking Lot

This document tracks future feature ideas, enhancements, and updates for consideration in upcoming releases.

## 🚀 Future Feature Ideas

### High Priority Features (Next Release)

#### 1. ✅ File Export Options (COMPLETED in v1.5.0-2.9.0)
**Status**: ✅ **COMPLETED**
**Implemented Features**:
- ✅ Multiple formats: JSON, CSV, XML, TXT, Kit Labels CSV
- ✅ Save to Downloads folder (local file generation)
- ✅ Share via Android intent system (email, messaging, cloud apps)
- ✅ Offline-first design - always generates files locally
- ✅ Date range selection and batch export
- ✅ AWS S3 integration with progress tracking
- ✅ Professional email formatting and SMS/MMS support
- ✅ Universal Export System across all features
**Version History**: v1.5.0 (JSON), v1.5.1 (Email/SMS), v1.6.0 (S3), v2.2.0 (Kit Labels), v2.7.0 (Unified System), v2.9.0 (S3 Upload Manager)

#### 2. Enhanced Offline Mode
**Type**: New Feature (Minor Version)
**Description**: Enhanced offline capabilities with automatic sync queue when connectivity returns
**Current Status**: ✅ **Partial** - Local export already works 100% offline
**What's Left**: Add sync queue for cloud uploads when connectivity returns
**Benefits**:
- Queue S3 uploads for later when offline
- Automatic retry with exponential backoff
- Better reliability for field use in remote locations
**Implementation**: Background sync service, queue management, conflict resolution

#### 3. ✅ Barcode Support (COMPLETED in v1.0+)
**Status**: ✅ **COMPLETED** 
**Implemented Features**:
- ✅ QR Codes (2D matrix barcodes)
- ✅ Code 128 (high-density linear barcode)
- ✅ Code 39 (alphanumeric linear barcode)
- ✅ Code 93 (compact linear barcode)
- ✅ UPC-A/E (Universal Product Codes)
- ✅ EAN-13/8 (European Article Numbers)
**Note**: This feature has been available since early versions - can be removed from future features list

### Medium Priority Features

#### 4. Live Scan Mode - Wi-Fi Companion Fallback
**Type**: Contingency Feature (only if Bluetooth HID is incompatible with target devices)
**Description**: If the primary Bluetooth HID approach for Live Scan is incompatible with
the Moto G 5G 2024 or Galaxy A14 5G, this is the fallback. The Android app sends scanned
values over Wi-Fi to a small helper app on the laptop, which uses OS-native keyboard
simulation to type the value at the cursor position.
**Status**: 🟠 **PARKED — pending Galaxy A14 5G test result**
**BT HID Compatibility Findings (2026-07-07)**:
- Moto G 5G 2024: ❌ FAILED — `BluetoothProfile.HID_DEVICE` service not present in OEM BT stack.
  Fast-fail detection (3-second watchdog) added in `HidKeyboardService` — phone now shows a
  clear error instead of hanging. See LIVE_SCAN_MODE_DESIGN.md for full diagnosis.
- Galaxy A14 5G: ⏳ NOT YET TESTED (device currently on loan)
**Trigger**: Build Wi-Fi fallback if Galaxy A14 5G also fails, OR if no compatible phone is
available within the deployment fleet when Phase 2 is needed.
**Architecture**:
- Android app: sends scanned string to local WebSocket server (Wi-Fi)
- Mac helper: tiny menu-bar app (Python + pynput, or Swift + CGEvent)
- Windows helper: tray app (Python + pyautogui, or C# + SendKeys)
**Trigger**: Build this if Phase 1 BT HID smoke test fails on both target devices
**Reference**: See LIVE_SCAN_MODE_DESIGN.md for full BT HID approach and risk table

#### 5. Bulk Scanning Mode
**Type**: New Feature (Minor Version)
**Description**: Enhanced rapid scanning interface for high-volume operations
**Status**: 🟡 **READY FOR DEVELOPMENT**
**Benefits**: 
- Process large batches of kit checkouts efficiently
- Reduce manual interaction time between scans
- Improve productivity for high-volume scanning sessions
- Minimize UI interactions during continuous scanning
**Key Requirements**:
- Rapid succession scanning with minimal delays
- Visual queue/list of scanned items
- Batch validation before committing
- Easy error correction and item removal
- Progress indicators and scan counters
- Configurable scan delays and timeouts
**Considerations**: UI/UX for batch processing, memory management for large batches, error handling, undo/redo functionality

#### 5. User Authentication
**Type**: New Feature (Minor Version)
**Description**: Integrate authentication to track individual users' checkouts
**Benefits**:
- Audit trail for who checked out which kits
- Personal accountability and tracking
- Integration potential with internal systems
**Considerations**: Authentication method (local vs. cloud), user management, privacy concerns

#### 6. Analytics Dashboard
**Type**: New Feature (Minor Version)
**Description**: Simple dashboard for monitoring kit checkout trends
**Features**:
- Total checkouts per day
- Popular kits tracking
- User activity reports
- Basic trend analysis
**Considerations**: Data visualization library, performance with large datasets

### Lower Priority Features

#### 7. Error Handling Improvements
**Type**: Enhancement (Patch Version)
**Description**: Enhanced error messaging with specific guidance and retry logic
**Benefits**:
- Better user experience when scans fail
- Reduced confusion and support requests
- Guidance for common issues (lighting, blur, etc.)

#### 8. Hardware Compatibility Optimization
**Type**: Enhancement (Patch Version)
**Description**: Optimize for different device cameras and form factors
**Benefits**:
- Reliable performance across devices
- Better support for various screen sizes
- Improved scanning accuracy
**Considerations**: Device testing, camera API optimization

#### 9. Voice Confirmation
**Type**: New Feature (Patch Version)
**Description**: Optional voice feedback for successful scans
**Benefits**:
- Hands-free confirmation
- Accessibility improvement
- Eyes-free operation capability
**Considerations**: Accessibility standards, audio permissions

#### 10. Cross-Platform Development
**Type**: Major Change (Major Version)
**Description**: Evaluate moving to Flutter or React Native
**Benefits**:
- Simultaneous iOS development
- Code reuse between platforms
- Broader market reach
**Considerations**: Migration effort, learning curve, performance impact

## 🎯 Enhancement Categories

### User Experience
- Bulk Scanning Mode
- Error Handling Improvements
- Voice Confirmation
- Hardware Compatibility

### Data Management
- File Import/Export Options
- Analytics Dashboard
- Offline Mode
- User Authentication

### Platform Expansion
- Cross-Platform Development
- Barcode Support
- iOS Compatibility

## 📋 Implementation Priority Matrix

| Feature | Impact | Effort | Priority |
|---------|--------|--------|----------|
| File Export Options | High | Medium | 🔴 High |
| Offline Mode | High | High | 🔴 High |
| Barcode Support | High | Medium | 🔴 High |
| Bulk Scanning Mode | Medium | Medium | 🟡 Medium |
| User Authentication | Medium | High | 🟡 Medium |
| Analytics Dashboard | Medium | High | 🟡 Medium |
| Error Handling | Medium | Low | 🟢 Low |
| Hardware Optimization | Medium | Low | 🟢 Low |
| Voice Confirmation | Low | Low | 🟢 Low |
| Cross-Platform (iOS + Tablet) | High | Very High | 🟠 Long-term |

## 🔄 Version Planning Strategy

### Next Minor Release (1.2.0)
- File Export Options (CSV, Excel compatibility)
- Offline Mode with sync capabilities

### Future Minor Release (1.3.0) 
- Barcode Support (Code 128, UPC, Code 39)
- Enhanced barcode scanning UI

### Future Minor Release (1.4.0)
- Bulk Scanning Mode
- User Authentication

### Future Minor Release (1.5.0) ✅ COMPLETED
- File Export Options (JSON format)
- Date range selection
- Save to Downloads
- Share via Android Intent
- Location-aware file naming

### Future Minor Release (1.5.1) ✅ COMPLETED
- Email Export with professional formatting
- SMS/MMS Export with file attachments
- Pre-filled email subjects and body content
- Warning dialog for SMS attachment limitations

### Future Minor Release (1.6.0)
- Analytics Dashboard
- Advanced reporting features

### Future Minor Release (1.7.0)
- S3 Bucket Integration
- Google Drive Integration
- Advanced retry logic

### Major Release Consideration (2.0.0)
- Cross-Platform Development (Flutter/React Native)
- iOS and tablet support
- Unified codebase for phone and tablet form factors
- Architectural overhaul for multi-platform support

## ✅ S3 Bucket Integration (COMPLETED v1.6.0-2.9.0)

### **Status**: ✅ **COMPLETED**
**Implemented Features**:
- ✅ Full S3 configuration UI in settings
- ✅ AWS region selection and credentials management  
- ✅ Custom bucket and folder configuration
- ✅ Connection testing functionality
- ✅ Direct upload with progress tracking
- ✅ Batch upload for multiple files
- ✅ Automatic retry with exponential backoff (3 attempts, 1s-10s delays)
- ✅ Metadata tagging (location, date, record count)
- ✅ Location-based folder structure: `/[LocationID]/[Year]/[Month]/`
- ✅ All export formats supported (JSON, CSV, XML, TXT)
- ✅ Universal S3 Upload Manager for all data types
- ✅ Network connectivity checking
- ✅ Comprehensive error handling and user feedback

**Version History**: v1.6.0 (Initial S3), v2.9.0 (Universal S3 Upload Manager)
**Authentication**: Access Key/Secret Key pairs with secure storage
**File Organization**: Implemented as designed with location-aware structure

## 🌐 Offline Export Strategies

### **Primary Export Methods (No Connectivity Required)**
1. **Local File Generation**: 
   - Generate CSV/Excel files directly to Downloads folder
   - Immediate access via Files app for manual transfer
   - Works 100% offline

2. **Android Share Intent**:
   - Tap "Export" → "Share" to access all available sharing options
   - Bluetooth transfer to nearby devices
   - Save to cloud apps (when WiFi available later)
   - Attach to drafts in email/messaging apps

3. **USB File Transfer**:
   - Connect phone to laptop/computer via USB
   - Access Downloads folder directly
   - Standard file transfer workflow

### **Secondary Methods (When Connectivity Available)**
4. **Automatic Cloud Sync**:
   - Queue exports for upload when connectivity returns
   - Background sync to Google Drive, Dropbox, etc.
   - Email automated reports

5. **WiFi Hotspot**:
   - Use vehicle/base station WiFi when available
   - Instant cloud uploads and email sharing

### **Recommended Workflow**
- **On-Site**: Generate and save files locally, use USB/Bluetooth transfer
- **Back at Base**: Automatic sync queued files to cloud/email systems
- **Emergency**: Share via satellite messaging (if available) for critical data

## 📝 Notes

- All features should maintain backward compatibility with existing JSON file format
- **Connectivity Independence**: Export functionality must work without cellular/WiFi
- Security considerations needed for any authentication implementation
- Performance testing required for bulk operations
- User feedback should drive priority adjustments
- Consider pilot community input before major changes
- **Field Testing**: Test export workflows in various connectivity scenarios

## 🧹 Code Cleanup & Maintenance

### Completed Fixes
- ✅ **Unused Variable Warning Fixed**: Fixed unused parameter warning in CheckInActivity (commit 3969137)
  - Type: Code cleanup (no functional impact)
  - Impact: Eliminated compiler warning
  - Status: Completed
  
- ✅ **Unused Imports Removed**: Cleaned up unused imports across activities (commit 2b44c5f)
  - CheckInActivity: Removed 7 unused imports
  - InventoryManagementActivity: Removed 8 unused imports  
  - KitBundleActivity: Removed 10 unused imports
  - Impact: Cleaner code, slightly faster compilation

- ✅ **Toast Calls Centralized**: Replaced direct Toast.makeText() with DialogUtils (commit 8593362)
  - SettingsActivity: 2 replacements
  - UnifiedExportActivity: 1 replacement
  - Impact: Consistent user messaging, follows SOLID principles

- ✅ **Theming Issues Fixed**: 
  - Fixed hardcoded colors in export dialog (black text invisible in dark mode)
  - Fixed hardcoded colors in settings screen (gray text hard to read on white)
  - Replaced with theme-aware colors (`?attr/colorOnSurface`, `?attr/colorOnSurfaceVariant`)

## 🧰 Housekeeping Backlog

### Git & File Cleanup
| Item | Type | Notes |
|------|------|-------|
| Commit or unstage `BUILD_INFO.txt` and `.idea/deploymentTargetSelector.xml` | Git hygiene | Currently staged but uncommitted |
| Delete `clinerules copy 2` (staged deletion) | Git hygiene | Already deleted locally, needs commit |
| Decide on `clinerules_QRScanner.md` (untracked) | Git hygiene | Duplicate of `.clinerules` — pick one canonical location and remove the other |
| Remove `.clinerules` if `clinerules_QRScanner.md` is kept (or vice versa) | Git hygiene | Two identical files with project rules |
| Create project-level `CLAUDE.md` from `clinerules_QRScanner.md` content | Tooling | Migrate Cline-specific rules to Claude Code format for this project |

### Documentation Discrepancies
| Item | Type | Notes |
|------|------|-------|
| Fix minSdk documentation mismatch | Docs | README says "API Level 24+" but `build.gradle` has `minSdk 28` — update README to match |
| BUILD_INFO.txt mentions AGP downgrade from 8.13.0 to 8.7.0 | Docs | Actual `build.gradle` doesn't show this; recent commits bumped to 8.13.2. Clarify or remove |
| REQUIREMENTS.md still references "API Level 24" in TR-001 | Docs | Should be updated to match actual minSdk 28 |
| Update PARKING_LOT "Last Updated" date | Docs | Currently says January 5, 2026 |

### Code Quality
| Item | Type | Notes |
|------|------|-------|
| Clear/Clear All buttons commented out in UI | Tech debt | Per v2.8.0 changelog — decide: restore or fully remove dead code |
| `ScanModeSelector.kt` still exists but unused | Tech debt | Replaced by discrete buttons in v2.5.2 — confirm unused and remove |
| 9 failing unit tests (per v2.7.5 notes) | Testing | 91% pass rate noted — fix remaining assertion adjustments |
| No tests for OCR pipeline or Kit Bundle logic | Testing | Core business logic with zero coverage |
| No tests for export data sources | Testing | `ExportDataSource` implementations untested |
| `ScanHistoryManager` implemented but UI integration pending | Feature gap | Infrastructure built in Dec 2025, never wired to UI |
| Live Scan BT HID — paused pending Galaxy A14 test | Feature gap | Phase 1 committed; blocked on device procurement |

### Build System
| Item | Type | Notes |
|------|------|-------|
| `copyApksToSharedLocation` task commented out | Build | Gradle compatibility issue — investigate if AGP 8.13.2 resolves it |
| Gradle wrapper version (8.10.2) vs AGP 8.13.2 compatibility | Build | Verify these are aligned; AGP 8.13+ may need Gradle 8.11+ |

## 📅 Last Updated
July 20, 2026 - Housekeeping backlog added, Live Scan paused pending device:
- ✅ COMPLETED v1.5.0-2.9.0: Complete File Export System with AWS S3 Integration
- ✅ COMPLETED v1.0+: Full Barcode Support (QR, Code 128, Code 39, UPC, EAN)
- ✅ COMPLETED v2.6.0: Inventory Management Mode (bulk device scanning up to 500 items)
- ✅ COMPLETED v2.1.0+: Advanced OCR with image preprocessing and confidence tuning
- ✅ COMPLETED v2.8.0: Material Design 3 theming system
- 🟡 PAUSED: Live Scan Mode (BT HID) — waiting on Galaxy A14 5G test
- 🎯 NEXT PRIORITY: Bulk Scanning Mode for rapid kit checkout operations
- 🎯 READY FOR DEVELOPMENT: User Authentication system
- Long-term: Cross-Platform Development (iOS + tablet support)

---
*This document should be updated regularly as new ideas emerge and priorities shift based on user feedback and business needs.*
