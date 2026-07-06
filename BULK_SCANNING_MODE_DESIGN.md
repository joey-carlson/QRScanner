# Bulk Scanning Mode - Design Document v1.0

## 📋 Requirements Analysis

### Current Scanning Limitations
- **Kit Check Out**: Requires review/confirmation step between each checkout pair
- **Inventory Management**: Already supports bulk scanning (up to 500 items) but single-type only
- **Individual Processing**: Each scan requires manual interaction and confirmation delays

### Bulk Scanning Mode Goals
- **Rapid Kit Checkouts**: Process large batches of user/kit pairs with minimal interaction
- **Queue-Based Processing**: Build up a list of checkouts before batch commit
- **Minimal Delays**: Eliminate confirmation dialogs and review steps during rapid scanning
- **Error Correction**: Easy removal/correction of incorrect scans
- **Batch Validation**: Review entire batch before final commit

## 🎯 Core Features

### 1. **Rapid Succession Scanning**
- Continuous scanning without pauses
- No review panel per checkout pair
- Immediate visual/haptic feedback for each scan
- Queue checkouts for batch processing

### 2. **Visual Queue Management**
- Scrollable list showing all scanned pairs
- Real-time status indicators (complete pairs vs pending)
- Easy identification of incomplete pairs (user without kit, kit without user)
- Quick removal of incorrect items

### 3. **Batch Operations**
- **Batch Commit**: Save all complete pairs at once
- **Batch Validation**: Pre-commit validation of all pairs
- **Batch Clear**: Clear entire session and start over
- **Selective Removal**: Remove individual items from queue

### 4. **Performance Optimizations**
- **Memory Management**: Handle large batches (100+ pairs) efficiently
- **Scanning Speed**: Reduced processing time between scans (<500ms)
- **UI Responsiveness**: Smooth scrolling and updates during rapid scanning

## 🏗️ Technical Architecture

### Data Models
```kotlin
data class BulkCheckoutItem(
    val id: String = UUID.randomUUID().toString(),
    val userId: String? = null,
    val kitId: String? = null,
    val timestamp: Instant = Instant.now(),
    val isComplete: Boolean = false
) {
    val status: ItemStatus
        get() = when {
            userId != null && kitId != null -> ItemStatus.COMPLETE
            userId != null -> ItemStatus.USER_ONLY
            kitId != null -> ItemStatus.KIT_ONLY
            else -> ItemStatus.EMPTY
        }
}

enum class ItemStatus {
    EMPTY,
    USER_ONLY,
    KIT_ONLY,
    COMPLETE
}

data class BulkCheckoutSession(
    val items: MutableList<BulkCheckoutItem> = mutableListOf(),
    val startTime: Instant = Instant.now()
) {
    val completeItems: List<BulkCheckoutItem>
        get() = items.filter { it.isComplete }
    
    val incompleteItems: List<BulkCheckoutItem>
        get() = items.filter { !it.isComplete }
    
    val totalItems: Int get() = items.size
    val completePairs: Int get() = completeItems.size
}
```

### ViewModel Architecture
```kotlin
class BulkScanViewModel : AndroidViewModel {
    private val session = MutableStateFlow(BulkCheckoutSession())
    private val statusMessage = MutableStateFlow("Ready for bulk scanning")
    private val isInBulkMode = MutableStateFlow(false)
    private val scanningSpeed = MutableStateFlow(ScanningSpeed.NORMAL)
    
    // Key functions:
    fun startBulkSession()
    fun addScanToBatch(barcodeData: String)
    fun removeBatchItem(itemId: String)
    fun commitBatch(): Flow<BatchCommitResult>
    fun clearBatch()
}
```

### UI Components
```kotlin
class BulkScanActivity : AppCompatActivity() {
    // Key components:
    // - Camera preview (top 40% of screen)
    // - Scan queue RecyclerView (middle 50% of screen)
    // - Action buttons (bottom 10% of screen)
}

class BulkScanAdapter : RecyclerView.Adapter<BulkScanViewHolder>() {
    // Features:
    // - Show complete pairs (green background)
    // - Show incomplete items (yellow background)
    // - Swipe-to-delete functionality
    // - Tap-to-edit functionality
}
```

## 🚀 Implementation Approach

### Phase 1: Core Bulk Scanning Infrastructure
**Target: v2.9.0 - Bulk Foundation**
- Create `BulkCheckoutItem` and `BulkCheckoutSession` data models
- Create `BulkScanViewModel` with session management
- Create basic `BulkScanActivity` with camera integration
- Implement rapid scanning without confirmations

### Phase 2: Queue Management UI
**Target: v2.10.0 - Queue Interface**
- Create `BulkScanAdapter` for displaying scan queue
- Implement item status indicators and color coding
- Add swipe-to-delete and tap-to-edit functionality
- Create batch operation buttons (Clear All, Commit Batch)

### Phase 3: Advanced Features
**Target: v2.11.0 - Advanced Bulk Features**
- Add scanning speed controls (Normal, Fast, Turbo)
- Implement batch validation with error highlighting
- Add progress indicators for batch commits
- Create bulk scanning statistics and performance metrics

### Phase 4: Integration & Polish
**Target: v2.12.0 - Production Ready**
- Integrate with UniversalExportManager
- Add comprehensive error handling and recovery
- Create unit tests for bulk scanning components
- Performance optimization and memory management

## 💡 User Experience Flow

### 1. **Entry Point**
- Add "Bulk Kit Check Out" button on Home Screen
- Position between "Kit Check Out" and "Kit Check In"

### 2. **Scanning Workflow**
```
Start Bulk Mode
    ↓
Continuous Scanning
    ↓ (User scan)
Add to Queue (Yellow - Incomplete)
    ↓ (Kit scan for same user)
Mark Complete (Green - Ready)
    ↓ (Repeat...)
Build Queue of Pairs
    ↓ (When done scanning)
Review Queue
    ↓ (Remove errors/duplicates)
Commit Batch
    ↓
Export Options
```

### 3. **Visual Design**
- **Top Section**: Camera preview with scan instructions
- **Middle Section**: Scrollable queue showing all scanned items
- **Bottom Section**: Action buttons (Clear, Commit, Export)
- **Status Bar**: Running count of complete pairs vs incomplete items

## ⚡ Performance Considerations

### Memory Management
- **Batch Size Limits**: Start with 100 pairs max, expand based on testing
- **Memory Cleanup**: Clear processed items after successful commit
- **Background Processing**: Use coroutines for batch operations

### Scanning Optimization
- **Reduced Delays**: Target <500ms between successful scans
- **Frame Throttling**: Process every 3rd frame for better performance
- **Duplicate Prevention**: Prevent same barcode from being scanned twice in rapid succession

### UI Responsiveness
- **Background Commits**: Use background threads for batch saves
- **Progress Indicators**: Show progress during long batch commits
- **Smooth Scrolling**: Optimize RecyclerView for rapid updates

## 🔗 Integration Points

### Existing Infrastructure to Leverage
1. **BarcodeValidator**: Use existing validation and security checks
2. **CheckoutRepository**: Extend with batch operations
3. **UniversalExportManager**: Integrate for consistent export experience
4. **HapticManager**: Provide appropriate feedback for bulk operations
5. **CameraManager**: Reuse existing camera setup and management
6. **WindowInsetsHelper**: Apply consistent system UI handling

### New Components Needed
1. **BulkScanActivity**: New activity for bulk scanning interface
2. **BulkScanViewModel**: Session management and batch processing
3. **BulkScanAdapter**: RecyclerView adapter for scan queue
4. **BulkCheckoutRepository**: Extended repository for batch operations
5. **Layout Resources**: New activity layout and list item layouts

## 📊 Success Metrics

### Performance Targets
- **Scanning Speed**: <500ms between scans
- **Batch Commit**: <2 seconds for 50 pairs
- **Memory Usage**: <50MB for 100 pair session
- **Battery Impact**: <5% additional drain per hour

### User Experience Goals
- **Error Rate**: <2% incorrect scans requiring manual correction
- **Task Completion**: 90% of bulk sessions result in successful batch commit
- **User Satisfaction**: Reduced scanning time by 60% vs individual mode

## 🚧 Implementation Notes

### Architecture Decisions
- **MVVM Pattern**: Consistent with existing app architecture
- **Repository Pattern**: Extend existing CheckoutRepository
- **Strategy Pattern**: Different bulk modes (speed vs accuracy)
- **Observer Pattern**: LiveData/StateFlow for UI updates

### Development Approach
- **Incremental Implementation**: Build and test each phase separately
- **Backward Compatibility**: Don't break existing checkout functionality
- **Testing Strategy**: Unit tests for data models, UI tests for user flow
- **Performance Testing**: Stress test with large batches

---

**Next Steps**: Review this design and determine which phase to start with. Phase 1 (Core Infrastructure) is recommended as it provides immediate value with minimal risk.
