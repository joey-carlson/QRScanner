# QR Scanner Requirements Document

## Document Control

**Document Version:** 1.6  
**Last Updated:** 2025-09-15  
**Author:** joecrls + Cline  
**Status:** Active  

### Document Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.6 | 2025-09-15 | joecrls + Cline | Added sophisticated OCR confidence tuning system with multi-factor scoring, environmental adaptation, and component-specific thresholds |
| 1.5 | 2025-09-05 | joecrls + Cline | Added Kit Bundle advanced features (smart detection, review panel, labels export), expanded export requirements, added architectural requirements |
| 1.4 | 2025-09-02 | joecrls + Cline | Updated UI labels for clarity (Kit Check Out/In), added User Check In placeholder |
| 1.3 | 2025-09-02 | joecrls + Cline | Added Check In feature requirements and specifications |
| 1.2 | 2025-08-30 | joecrls + Cline | Added OCR capabilities for Device Serial Number (DSN) recognition |
| 1.1 | 2025-08-30 | joecrls + Cline | Added implementation decisions for Kit Bundle data layer |
| 1.0 | 2025-08-30 | joecrls + Cline | Initial requirements document creation including Check Out feature (existing) and Kit Bundle feature (new) |

---

## 1. Project Overview & Objectives

### Version 1.5 (2025-09-05)
**Changes:** Updated project scope to reflect advanced features and architectural improvements

### Version 1.0 (2025-08-30)
**Initial Version**

#### Project Purpose
The QR Scanner application is designed to streamline the process of tracking equipment checkouts, check-ins, and managing kit component bundles through QR code scanning. The system provides a mobile solution for field personnel to quickly record kit movements and bundle component tracking with advanced OCR capabilities and comprehensive export options.

#### Project Scope
- Android mobile application for QR/barcode/OCR scanning
- Local JSON data storage with export capabilities
- Four primary features: Kit Check Out, Kit Check In, Kit Bundle management, and User Check In (placeholder)
- Advanced export functionality to multiple formats and destinations including AWS S3
- Smart component detection with confidence-based verification
- Comprehensive architectural patterns for maintainability

#### Success Criteria
- Rapid QR code scanning with immediate feedback
- Reliable local data storage with MediaStore compatibility
- Easy export of collected data for import into DynamoDB and other systems
- Intuitive user interface requiring minimal training
- High accuracy OCR text recognition for printed serial numbers
- Robust export system supporting multiple formats and destinations

#### Primary Stakeholders
- Field personnel (primary users)
- Inventory management team (data consumers)
- IT/Development team (maintainers)
- Label printing operations (kit labels export users)

---

## 2. Functional Requirements

### Version 1.5 (2025-09-05)
**Changes:** Added Kit Bundle advanced features, expanded export requirements

### Version 1.0 (2025-08-30)
**Initial Version**

### 2.1 Check Out Feature

#### Purpose
Enable users to scan QR codes for both users and kits to record checkout transactions.

#### Requirements
- **FR-CO-001**: System shall scan user QR codes (starting with "U" or "USER")
- **FR-CO-002**: System shall scan kit QR codes (any alphanumeric format not starting with "U" or "USER")
- **FR-CO-003**: System shall accept user and kit scans in any order
- **FR-CO-004**: System shall automatically save checkout record when both codes are scanned
- **FR-CO-005**: System shall provide visual confirmation of successful checkout
- **FR-CO-006**: System shall allow clearing of partial scan state
- **FR-CO-007**: System shall support undo of last checkout within timeout period
- **FR-CO-008**: System shall save checkout data to JSON files with date and location naming
- **FR-CO-009**: System shall provide haptic feedback on successful scans (NEW v1.5)
- **FR-CO-010**: System shall show checkout confirmation overlay with details (NEW v1.5)

### 2.2 Kit Bundle Feature

#### Purpose
Enable users to scan individual component codes and assign them to a kit bundle for tracking component-to-kit relationships.

#### Basic Requirements (v1.0)
- **FR-KB-001**: System shall scan kit QR code and append current MM/DD to create unique Kit ID
- **FR-KB-002**: System shall support scanning up to 8 components per kit bundle
- **FR-KB-003**: System shall support these component types:
  - Glasses
  - Controller
  - Battery 01
  - Battery 02
  - Battery 03
  - Pads
  - Unused 01
  - Unused 02
- **FR-KB-004**: System shall allow any component field to be null/blank
- **FR-KB-005**: System shall save kit bundle data to separate JSON files (qr_kits_MM-dd-yy_LocationID.json)
- **FR-KB-006**: System shall treat each kit bundle as immutable (no editing after creation)
- **FR-KB-007**: Component replacement creates new kit bundle with same base kit code but new MM/DD suffix

#### Smart Detection Requirements (NEW v1.5)
- **FR-KB-008**: System shall implement three-tier confidence detection:
  - HIGH (>90%): Automatic component assignment
  - MEDIUM (70-90%): Component confirmation dialog
  - LOW (<70%): Manual selection dialog
- **FR-KB-009**: System shall detect component type from DSN patterns
- **FR-KB-010**: System shall prevent duplicate DSN within kit session
- **FR-KB-011**: System shall track and display kit completion progress
- **FR-KB-012**: System shall validate minimum requirements (1 glasses, 1 controller, 2+ batteries)

#### Review Panel Requirements (NEW v1.5)
- **FR-KB-013**: System shall provide manual review panel for kit bundles
- **FR-KB-014**: System shall allow manual entry/editing of kit code and all components
- **FR-KB-015**: System shall validate all entries before saving
- **FR-KB-016**: System shall show review panel with all components before final save

#### Export Requirements (NEW v1.5)
- **FR-KB-017**: System shall export kit labels in single-column CSV format
- **FR-KB-018**: System shall map component names for label printing:
  - Controller → "Puck"
  - Glasses → "G"
- **FR-KB-019**: System shall generate numbered labels based on kit number
- **FR-KB-020**: System shall provide menu access to kit labels export

### 2.3 Check In Feature (v1.3)

#### Purpose
Enable users to scan kit QR codes to mark kits as "Checked In" when they are returned, without requiring a user code.

#### Requirements
- **FR-CI-001**: System shall scan kit QR codes for check-in (no user code required)
- **FR-CI-002**: System shall immediately save check-in record upon kit scan
- **FR-CI-003**: System shall provide visual confirmation "CHECK-IN COMPLETE" overlay
- **FR-CI-004**: System shall provide haptic feedback on successful scan
- **FR-CI-005**: System shall support undo of last check-in
- **FR-CI-006**: System shall allow clearing all check-ins for bulk operations
- **FR-CI-007**: System shall save check-in data to JSON files (qr_checkins_MM-dd-yy_LocationID.json)
- **FR-CI-008**: System shall support full export capabilities matching Check Out feature
- **FR-CI-009**: System shall display kit-only status without user field
- **FR-CI-010**: System shall maintain same performance and visual feedback as Kit Check Out mode

### 2.4 User Check In Feature (PLACEHOLDER v1.4)

#### Purpose
Placeholder for future functionality to enable tracking of users returning equipment.

#### Requirements
- **FR-UCI-001**: System shall display "User Check In" button on home screen
- **FR-UCI-002**: System shall show "Coming Soon" message when accessed
- **FR-UCI-003**: Placeholder shall maintain consistent UI with other features
- **FR-UCI-004**: Future implementation will track user equipment returns

### 2.5 OCR Capabilities (v1.2)

#### Purpose
Enable scanning of printed Device Serial Numbers (DSN) and other text identifiers when QR codes or barcodes are not available on components.

#### Requirements
- **FR-OCR-001**: System shall support three scanning modes:
  - Barcode Only (default)
  - OCR Only
  - Hybrid (automatic detection)
- **FR-OCR-002**: System shall recognize printed Device Serial Numbers (DSN) using ML Kit Text Recognition
- **FR-OCR-003**: System shall provide scan mode switching UI controls
- **FR-OCR-004**: System shall validate recognized text against known DSN patterns
- **FR-OCR-005**: System shall show confidence indicator for OCR results
- **FR-OCR-006**: System shall allow manual verification/editing of OCR results
- **FR-OCR-007**: System shall fall back to manual text entry when both barcode and OCR fail
- **FR-OCR-008**: System shall provide different visual feedback for barcode vs OCR recognition
- **FR-OCR-009**: OCR shall be available for both kit codes and component codes
- **FR-OCR-010**: System shall support component type inference from DSN patterns

### 2.6 Export System

#### Basic Requirements (v1.0)
- **FR-EX-001**: System shall provide date range selection for exports
- **FR-EX-002**: System shall support JSON and CSV export formats
- **FR-EX-003**: System shall support local storage export
- **FR-EX-004**: System shall support share/send via other apps
- **FR-EX-005**: System shall support AWS S3 export (when configured)
- **FR-EX-006**: System shall include location ID in exported filenames
- **FR-EX-007**: Export system shall handle checkout, check-in, and kit bundle data
- **FR-EX-008**: System shall generate separate files for each day in date range

#### Advanced Requirements (NEW v1.5)
- **FR-EX-009**: System shall support additional export formats:
  - XML (S3 only)
  - TXT (plain text)
  - Kit Labels CSV (single-column)
- **FR-EX-010**: System shall support email export with:
  - Pre-filled subject and body
  - Multiple file attachments
  - Professional formatting
- **FR-EX-011**: System shall support SMS/MMS export with:
  - File attachments via MMS
  - Carrier compatibility warnings
- **FR-EX-012**: System shall implement strategy pattern for extensible export methods
- **FR-EX-013**: System shall provide progress tracking for large exports
- **FR-EX-014**: System shall implement automatic retry for failed S3 uploads
- **FR-EX-015**: System shall support S3 metadata tagging

### 2.7 Navigation & User Interface

#### Requirements
- **FR-UI-001**: System shall provide home screen with feature selection (Kit Check Out / Kit Check In / User Check In / Kit Bundle)
- **FR-UI-002**: System shall provide navigation back to home from any feature
- **FR-UI-003**: System shall provide settings screen for configuration
- **FR-UI-004**: System shall display current app version and build number
- **FR-UI-005**: System shall provide menu access in Kit Bundle mode (NEW v1.5)
- **FR-UI-006**: System shall show visual progress indicators for multi-step operations (NEW v1.5)

---

## 3. Technical Requirements

### Version 1.5 (2025-09-05)
**Changes:** Added architectural requirements, enhanced technical specifications

### Version 1.0 (2025-08-30)
**Initial Version**

### 3.1 Platform Requirements
- **TR-001**: Android 7.0 (API Level 24) minimum
- **TR-002**: Target Android 14 (API Level 34)
- **TR-003**: Kotlin programming language
- **TR-004**: Material Design 3 UI components

### 3.2 Hardware Requirements
- **TR-005**: Rear-facing camera with autofocus capability
- **TR-006**: Minimum 2GB RAM recommended
- **TR-007**: Minimum 100MB available storage
- **TR-008**: Vibration motor for haptic feedback (NEW v1.5)

### 3.3 Performance Requirements
- **TR-008**: QR code recognition within 2 seconds under normal lighting
- **TR-009**: App launch to scan-ready state within 3 seconds
- **TR-010**: Export processing for 1000 records within 5 seconds
- **TR-011**: Component detection confidence calculation within 500ms (NEW v1.5)
- **TR-012**: S3 upload with progress tracking and retry logic (NEW v1.5)

### 3.4 Security Requirements
- **TR-011**: No user authentication required (per current scope)
- **TR-012**: Data stored in app-private storage
- **TR-013**: Input sanitization for scanned values
- **TR-014**: AWS credentials stored securely when configured
- **TR-015**: Comprehensive barcode validation to prevent injection attacks (NEW v1.5)

### 3.5 OCR Technical Requirements (v1.2)
- **TR-OCR-001**: ML Kit Text Recognition dependency (com.google.mlkit:text-recognition)
- **TR-OCR-002**: Image preprocessing for optimal OCR (rotation correction, contrast enhancement)
- **TR-OCR-003**: OCR processing within 3 seconds per scan attempt
- **TR-OCR-004**: Minimum 80% confidence threshold for automatic acceptance
- **TR-OCR-005**: Support for multiple text orientations (0°, 90°, 180°, 270°)
- **TR-OCR-006**: Text size detection range: 8pt to 48pt fonts
- **TR-OCR-007**: Support for alphanumeric characters and common separators (-, _, /, .)

### 3.6 Architectural Requirements (NEW v1.5)
- **TR-ARCH-001**: MVVM architecture with ViewModels and ViewModelFactories
- **TR-ARCH-002**: Repository pattern for data persistence
- **TR-ARCH-003**: Strategy pattern for export functionality
- **TR-ARCH-004**: Factory pattern for object creation
- **TR-ARCH-005**: Observer pattern using LiveData and StateFlow
- **TR-ARCH-006**: Centralized configuration using AppConfig and PreferenceKeys
- **TR-ARCH-007**: Unified FileManager for Android version compatibility
- **TR-ARCH-008**: Comprehensive validation systems (BarcodeValidator, DsnValidator)

---

## 4. Data Management Requirements

### Version 1.5 (2025-09-05)
**Changes:** Added haptic feedback manager, export strategy models

### Version 1.1 (2025-08-30)
**Changes:** Added implementation decisions for Kit Bundle data layer

### Version 1.0 (2025-08-30)
**Initial Version**

### 4.1 Data Models

#### Checkout Record
```json
{
    "user": "USER123",
    "kit": "KIT456",
    "type": "CHECKOUT",
    "value": "User USER123 checked out Kit KIT456",
    "timestamp": "2025-08-30T10:30:00Z"
}
```

#### Check-In Record (v1.3)
```json
{
    "kit": "KIT456",
    "type": "CHECKIN",
    "value": "KIT456",
    "timestamp": "2025-09-02T15:45:00Z"
}
```

#### Kit Bundle Record
```json
{
    "kitId": "K123-08/30",
    "baseKitCode": "K123",
    "creationDate": "08/30",
    "glasses": "GL456",
    "controller": "CTRL789",
    "battery01": "BAT001",
    "battery02": "BAT002",
    "battery03": null,
    "pads": "PAD123",
    "unused01": null,
    "unused02": null,
    "timestamp": "2025-08-30T10:30:00Z"
}
```

#### Implementation Decisions (v1.1)
- **KitBundle Data Class**: Implemented with Gson serialization annotations
- **Validation**: `isValid()` method requires at least one non-null component
- **Helper Methods**: 
  - `getFilledComponentCount()`: Returns count of assigned components
  - `getComponentList()`: Returns list of component name-value pairs
  - `generateKitId()`: Creates Kit ID with MM/dd date format
- **Repository Pattern**: `KitRepository` follows same pattern as `CheckoutRepository`
- **Storage**: MediaStore for Android 10+, direct file access for older versions
- **Configuration**: Added `KIT_BUNDLE_FILE_PREFIX` and `DATE_FORMAT_KIT_ID` to AppConfig

#### Advanced Implementation (NEW v1.5)
- **Component Detection Models**: 
  - `ComponentDetectionResult`: Confidence-based detection results
  - `KitRequirements`: Tracks minimum component requirements
  - `ComponentSlot`: Manages available component slots
- **Export Models**:
  - `ExportResult`: Sealed class for export outcomes
  - `ExportStrategy`: Interface for different export methods
  - `ValidationResult`: Export validation results
- **UI State Management**:
  - `KitBundleState`: Comprehensive state management
  - `ScanState`: Tracks scanning workflow states

### 4.2 File Naming Conventions
- **DR-001**: Checkout files: `qr_checkouts_MM-dd-yy_LocationID.json`
- **DR-002**: Kit bundle files: `qr_kits_MM-dd-yy_LocationID.json`
- **DR-003**: Check-in files: `qr_checkins_MM-dd-yy_LocationID.json`
- **DR-004**: Location ID required for all file operations
- **DR-005**: Date format: MM-dd-yy (month-day-year)
- **DR-006**: Kit labels files: `kit_labels_MM-DD_DeviceName_LocationID.csv` (NEW v1.5)

### 4.3 Data Persistence
- **DR-007**: Checkout data organized by date
- **DR-008**: Kit bundle data treated as permanent records
- **DR-009**: No data deletion except through undo feature
- **DR-010**: All data stored in JSON format
- **DR-011**: MediaStore used for Android 10+ compatibility (NEW v1.5)
- **DR-012**: Temporary files managed by TempFileManager (NEW v1.5)

---

## 5. User Interface Requirements

### Version 1.5 (2025-09-05)
**Changes:** Added Kit Bundle advanced UI requirements

### Version 1.0 (2025-08-30)
**Initial Version**

### 5.1 Screen Flow
1. **Home Screen**
   - Feature selection buttons (Kit Check Out / Kit Check In / User Check In / Kit Bundle)
   - Settings access
   - Version display

2. **Check Out Screen**
   - Camera preview
   - Scan status display
   - Clear button
   - Undo button (time-limited)
   - Export button
   - Checkout confirmation overlay (NEW v1.5)

3. **Kit Bundle Screen**
   - Kit ID display
   - Component scanning interface
   - Component list with status
   - Save button
   - Clear/New Kit button
   - Scan mode selector (NEW v1.5)
   - Review panel (NEW v1.5)
   - Progress indicators (NEW v1.5)
   - Menu access (NEW v1.5)

4. **Export Screen**
   - Date range selection
   - Export method selection
   - Progress indication
   - Success/failure feedback
   - Format selection (NEW v1.5)

5. **Settings Screen**
   - Location ID configuration
   - Device name display
   - S3 configuration (if applicable)
   - Connection testing (NEW v1.5)

### 5.2 Visual Feedback
- **UI-001**: White flash for successful scans
- **UI-002**: Red flash for invalid/failed scans
- **UI-003**: Haptic feedback for scan events
- **UI-004**: Clear status messages for all operations
- **UI-005**: Visual confirmation overlay for checkouts
- **UI-006**: Progress bars for OCR confidence (NEW v1.5)
- **UI-007**: Color-coded confidence indicators (NEW v1.5)
- **UI-008**: Component requirement progress display (NEW v1.5)

### 5.3 Dialogs and Overlays (NEW v1.5)
- **UI-009**: Component confirmation dialog for medium confidence
- **UI-010**: Component selection dialog for manual override
- **UI-011**: OCR verification dialog with editable text
- **UI-012**: Email/SMS warning dialogs
- **UI-013**: S3 upload progress dialogs

---

## 6. Business Rules

### Version 1.5 (2025-09-05)
**Changes:** Added smart detection and export rules

### Version 1.0 (2025-08-30)
**Initial Version**

### 6.1 Kit Bundle Rules
- **BR-001**: Kit bundles are immutable once created
- **BR-002**: Component replacement requires creating new kit bundle
- **BR-003**: Kit ID format: `<BaseKitCode>-<MM/DD>` for uniqueness
- **BR-004**: Components can belong to different kits on different days
- **BR-005**: Maximum 8 components per kit bundle
- **BR-006**: Minimum requirements: 1 glasses, 1 controller, 2+ batteries (NEW v1.5)
- **BR-007**: No duplicate DSN within same kit session (NEW v1.5)
- **BR-008**: Unused component slots excluded from exports (NEW v1.5)

### 6.2 Checkout Rules
- **BR-009**: One user can check out multiple kits
- **BR-010**: One kit can be checked out by multiple users (on different occasions)
- **BR-011**: Checkout records are immutable except for undo operation
- **BR-012**: Undo available for 30 seconds after checkout

### 6.3 Data Validation
- **BR-013**: User codes must start with "U" or "USER"
- **BR-014**: All scanned values sanitized for JSON compatibility
- **BR-015**: Maximum scan value length: 200 characters
- **BR-016**: DSN patterns validated against known formats (NEW v1.5)
- **BR-017**: Component type inference from DSN patterns (NEW v1.5)

### 6.4 Export Rules (NEW v1.5)
- **BR-018**: Location ID required for all exports
- **BR-019**: Maximum 31 days per export operation
- **BR-020**: Kit labels use simplified component naming
- **BR-021**: S3 uploads include metadata tags
- **BR-022**: Failed S3 uploads retry automatically

---

## 7. Integration Requirements

### Version 1.5 (2025-09-05)
**Changes:** Expanded export integration requirements

### Version 1.0 (2025-08-30)
**Initial Version**

### 7.1 Export Integration
- **IR-001**: JSON export format compatible with DynamoDB import
- **IR-002**: CSV export format compatible with Excel/spreadsheet tools
- **IR-003**: File sharing compatible with email, Slack, and other apps
- **IR-004**: Kit labels CSV compatible with label printer software (NEW v1.5)
- **IR-005**: XML format for structured data exchange (NEW v1.5)
- **IR-006**: TXT format for simple viewing (NEW v1.5)

### 7.2 AWS S3 Integration
- **IR-007**: Support for S3 bucket configuration
- **IR-008**: Support for AWS Cognito authentication
- **IR-009**: Automatic retry for failed uploads
- **IR-010**: Progress indication for uploads
- **IR-011**: S3 connection testing functionality (NEW v1.5)
- **IR-012**: Metadata tagging for S3 objects (NEW v1.5)
- **IR-013**: Custom folder structure support (NEW v1.5)

### 7.3 Communication Integration (NEW v1.5)
- **IR-014**: Email integration with attachment support
- **IR-015**: SMS/MMS integration with carrier warnings
- **IR-016**: Android share intent compatibility
- **IR-017**: Multiple file attachment support

### 7.4 Future Integration Considerations
- **IR-018**: JSON structure designed for DynamoDB compatibility
- **IR-019**: No current requirement for real-time synchronization
- **IR-020**: No current requirement for external API integration

---

## 8. Testing Requirements

### Version 1.0 (2025-08-30)
**Initial Version**

### 8.1 Unit Testing
- **TEST-001**: Data model validation tests
- **TEST-002**: Barcode validation logic tests
- **TEST-003**: File naming convention tests
- **TEST-004**: Export format generation tests
- **TEST-005**: Component detection confidence tests (NEW v1.5)
- **TEST-006**: DSN pattern validation tests (NEW v1.5)

### 8.2 Integration Testing
- **TEST-007**: Camera integration tests
- **TEST-008**: File system operation tests
- **TEST-009**: S3 upload tests (when configured)
- **TEST-010**: Export workflow end-to-end tests
- **TEST-011**: OCR text recognition tests (NEW v1.5)
- **TEST-012**: Email/SMS integration tests (NEW v1.5)

### 8.3 User Acceptance Criteria
- **TEST-013**: Scan 100 QR codes with 98% success rate
- **TEST-014**: Export 1000 records successfully
- **TEST-015**: Complete checkout workflow in under 10 seconds
- **TEST-016**: Complete kit bundle workflow in under 2 minutes
- **TEST-017**: OCR recognition accuracy >80% for printed DSN (NEW v1.5)
- **TEST-018**: Component detection accuracy >90% (NEW v1.5)

---

## 9. Deployment & Maintenance

### Version 1.0 (2025-08-30)
**Initial Version**

### 9.1 Build & Deployment
- **DM-001**: Automated APK generation with version naming
- **DM-002**: Automatic copy to WorkDocs on build
- **DM-003**: Debug and release build variants
- **DM-004**: Signed APK for production deployment

### 9.2 Version Management
- **DM-005**: Semantic versioning (MAJOR.MINOR.PATCH)
- **DM-006**: Build number increment for each build
- **DM-007**: Version display in app and filename

### 9.3 Documentation
- **DM-008**: README.md maintenance
- **DM-009**: CHANGELOG.md for version history
- **DM-010**: REQUIREMENTS.md (this document) versioning
- **DM-011**: Automatic documentation distribution via build process
- **DM-012**: PARKING_LOT.md for future feature tracking

---

## 10. Future Considerations

### Version 1.0 (2025-08-30)
**Initial Version**

### 10.1 Planned Enhancements
- Integration with inventory management systems
- User authentication and authorization
- Cloud synchronization beyond export
- Bulk scanning capabilities
- NFC tag support
- User Check In implementation

### 10.2 Technical Debt
- Consider migration to Jetpack Compose for UI
- Implement comprehensive error tracking
- Add analytics for usage patterns
- Optimize for larger datasets
- Consider dependency injection framework

### 10.3 Scalability Considerations
- Current design supports thousands of records
- JSON format allows easy schema evolution
- Modular architecture supports feature additions
- Strategy pattern enables new export methods
- Repository pattern allows data source flexibility

---

## Appendix A: Glossary

- **Kit**: A collection of equipment items that are checked out together
- **Component**: Individual items that make up a kit bundle
- **Check Out**: The process of recording a user taking possession of a kit
- **Check In**: The process of recording a kit being returned
- **Kit Bundle**: A defined set of components associated with a kit ID
- **Location ID**: Unique identifier for the scanning location/site
- **DSN**: Device Serial Number - printed identifier on components
- **OCR**: Optical Character Recognition - technology for reading printed text
- **Confidence Level**: Measure of certainty in component detection (High/Medium/Low)
- **Strategy Pattern**: Design pattern for interchangeable algorithms

---

## Appendix B: References

- Android Developer Documentation: https://developer.android.com
- Material Design Guidelines: https://material.io
- AWS SDK Documentation: https://aws.amazon.com/sdk-for-java/
- ML Kit Documentation: https://developers.google.com/ml-kit
- Project Repository: https://github.com/joey-carlson/QRScanner
