# QR Scanner Requirements Document

## Document Control

**Document Version:** 1.2  
**Last Updated:** 2025-08-30  
**Author:** Development Team  
**Status:** Active  

### Document Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.2 | 2025-08-30 | Development Team | Added OCR capabilities for Device Serial Number (DSN) recognition |
| 1.1 | 2025-08-30 | Development Team | Added implementation decisions for Kit Bundle data layer |
| 1.0 | 2025-08-30 | Development Team | Initial requirements document creation including Check Out feature (existing) and Kit Bundle feature (new) |

---

## 1. Project Overview & Objectives

### Version 1.0 (2025-08-30)
**Initial Version**

#### Project Purpose
The QR Scanner application is designed to streamline the process of tracking equipment checkouts and managing kit component bundles through QR code scanning. The system provides a mobile solution for field personnel to quickly record user-kit checkouts and bundle component tracking.

#### Project Scope
- Android mobile application for QR/barcode scanning
- Local JSON data storage with export capabilities
- Two primary features: Check Out tracking and Kit Bundle management
- Export functionality to multiple formats and destinations

#### Success Criteria
- Rapid QR code scanning with immediate feedback
- Reliable local data storage
- Easy export of collected data for import into DynamoDB
- Intuitive user interface requiring minimal training

#### Primary Stakeholders
- Field personnel (primary users)
- Inventory management team (data consumers)
- IT/Development team (maintainers)

---

## 2. Functional Requirements

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

### 2.2 Kit Bundle Feature (NEW)

#### Purpose
Enable users to scan individual component codes and assign them to a kit bundle for tracking component-to-kit relationships.

#### Requirements
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

### 2.3 OCR Capabilities (NEW v1.2)

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

### 2.3 Export System

#### Requirements
- **FR-EX-001**: System shall provide date range selection for exports
- **FR-EX-002**: System shall support JSON and CSV export formats
- **FR-EX-003**: System shall support local storage export
- **FR-EX-004**: System shall support share/send via other apps
- **FR-EX-005**: System shall support AWS S3 export (when configured)
- **FR-EX-006**: System shall include location ID in exported filenames
- **FR-EX-007**: Export system shall handle both checkout and kit bundle data
- **FR-EX-008**: System shall generate separate files for each day in date range

### 2.4 Navigation & User Interface

#### Requirements
- **FR-UI-001**: System shall provide home screen with feature selection (Check Out / Kit Bundle)
- **FR-UI-002**: System shall provide navigation back to home from any feature
- **FR-UI-003**: System shall provide settings screen for configuration
- **FR-UI-004**: System shall display current app version and build number

---

## 3. Technical Requirements

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

### 3.3 Performance Requirements
- **TR-008**: QR code recognition within 2 seconds under normal lighting
- **TR-009**: App launch to scan-ready state within 3 seconds
- **TR-010**: Export processing for 1000 records within 5 seconds

### 3.4 Security Requirements
- **TR-011**: No user authentication required (per current scope)
- **TR-012**: Data stored in app-private storage
- **TR-013**: Input sanitization for scanned values
- **TR-014**: AWS credentials stored securely when configured

### 3.5 OCR Technical Requirements (NEW v1.2)
- **TR-OCR-001**: ML Kit Text Recognition dependency (com.google.mlkit:text-recognition)
- **TR-OCR-002**: Image preprocessing for optimal OCR (rotation correction, contrast enhancement)
- **TR-OCR-003**: OCR processing within 3 seconds per scan attempt
- **TR-OCR-004**: Minimum 80% confidence threshold for automatic acceptance
- **TR-OCR-005**: Support for multiple text orientations (0°, 90°, 180°, 270°)
- **TR-OCR-006**: Text size detection range: 8pt to 48pt fonts
- **TR-OCR-007**: Support for alphanumeric characters and common separators (-, _, /, .)

---

## 4. Data Management Requirements

### Version 1.1 (2025-08-30)
**Changes:** Added implementation decisions for Kit Bundle data layer
- Documented KitBundle.kt data class implementation
- Added KitRepository.kt implementation details
- Specified validation and helper methods
- Added configuration constants

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

### 4.2 File Naming Conventions
- **DR-001**: Checkout files: `qr_checkouts_MM-dd-yy_LocationID.json`
- **DR-002**: Kit bundle files: `qr_kits_MM-dd-yy_LocationID.json`
- **DR-003**: Location ID required for all file operations
- **DR-004**: Date format: MM-dd-yy (month-day-year)

### 4.3 Data Persistence
- **DR-005**: Checkout data organized by date
- **DR-006**: Kit bundle data treated as permanent records
- **DR-007**: No data deletion except through undo feature
- **DR-008**: All data stored in JSON format

---

## 5. User Interface Requirements

### Version 1.0 (2025-08-30)
**Initial Version**

### 5.1 Screen Flow
1. **Home Screen**
   - Feature selection buttons (Check Out / Kit Bundle)
   - Settings access
   - Version display

2. **Check Out Screen**
   - Camera preview
   - Scan status display
   - Clear button
   - Undo button (time-limited)
   - Export button

3. **Kit Bundle Screen**
   - Kit ID display
   - Component scanning interface
   - Component list with status
   - Save button
   - Clear/New Kit button

4. **Export Screen**
   - Date range selection
   - Export method selection
   - Progress indication
   - Success/failure feedback

5. **Settings Screen**
   - Location ID configuration
   - Device name display
   - S3 configuration (if applicable)

### 5.2 Visual Feedback
- **UI-001**: White flash for successful scans
- **UI-002**: Red flash for invalid/failed scans
- **UI-003**: Haptic feedback for scan events
- **UI-004**: Clear status messages for all operations
- **UI-005**: Visual confirmation overlay for checkouts

---

## 6. Business Rules

### Version 1.0 (2025-08-30)
**Initial Version**

### 6.1 Kit Bundle Rules
- **BR-001**: Kit bundles are immutable once created
- **BR-002**: Component replacement requires creating new kit bundle
- **BR-003**: Kit ID format: `<BaseKitCode>-<MM/DD>` for uniqueness
- **BR-004**: Components can belong to different kits on different days
- **BR-005**: Maximum 8 components per kit bundle

### 6.2 Checkout Rules
- **BR-006**: One user can check out multiple kits
- **BR-007**: One kit can be checked out by multiple users (on different occasions)
- **BR-008**: Checkout records are immutable except for undo operation
- **BR-009**: Undo available for 30 seconds after checkout

### 6.3 Data Validation
- **BR-010**: User codes must start with "U" or "USER"
- **BR-011**: All scanned values sanitized for JSON compatibility
- **BR-012**: Maximum scan value length: 200 characters

---

## 7. Integration Requirements

### Version 1.0 (2025-08-30)
**Initial Version**

### 7.1 Export Integration
- **IR-001**: JSON export format compatible with DynamoDB import
- **IR-002**: CSV export format compatible with Excel/spreadsheet tools
- **IR-003**: File sharing compatible with email, Slack, and other apps

### 7.2 AWS S3 Integration
- **IR-004**: Support for S3 bucket configuration
- **IR-005**: Support for AWS Cognito authentication
- **IR-006**: Automatic retry for failed uploads
- **IR-007**: Progress indication for uploads

### 7.3 Future Integration Considerations
- **IR-008**: JSON structure designed for DynamoDB compatibility
- **IR-009**: No current requirement for real-time synchronization
- **IR-010**: No current requirement for external API integration

---

## 8. Testing Requirements

### Version 1.0 (2025-08-30)
**Initial Version**

### 8.1 Unit Testing
- **TEST-001**: Data model validation tests
- **TEST-002**: Barcode validation logic tests
- **TEST-003**: File naming convention tests
- **TEST-004**: Export format generation tests

### 8.2 Integration Testing
- **TEST-005**: Camera integration tests
- **TEST-006**: File system operation tests
- **TEST-007**: S3 upload tests (when configured)
- **TEST-008**: Export workflow end-to-end tests

### 8.3 User Acceptance Criteria
- **TEST-009**: Scan 100 QR codes with 98% success rate
- **TEST-010**: Export 1000 records successfully
- **TEST-011**: Complete checkout workflow in under 10 seconds
- **TEST-012**: Complete kit bundle workflow in under 2 minutes

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

### 10.2 Technical Debt
- Consider migration to Jetpack Compose for UI
- Implement comprehensive error tracking
- Add analytics for usage patterns
- Optimize for larger datasets

### 10.3 Scalability Considerations
- Current design supports thousands of records
- JSON format allows easy schema evolution
- Modular architecture supports feature additions

---

## Appendix A: Glossary

- **Kit**: A collection of equipment items that are checked out together
- **Component**: Individual items that make up a kit bundle
- **Check Out**: The process of recording a user taking possession of a kit
- **Kit Bundle**: A defined set of components associated with a kit ID
- **Location ID**: Unique identifier for the scanning location/site

---

## Appendix B: References

- Android Developer Documentation: https://developer.android.com
- Material Design Guidelines: https://material.io
- AWS SDK Documentation: https://aws.amazon.com/sdk-for-java/
- Project Repository: https://github.com/joey-carlson/QRScanner
