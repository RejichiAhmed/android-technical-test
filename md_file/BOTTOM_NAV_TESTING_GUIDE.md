# Bottom Navigation Bar - Quick Testing Guide

## 🚀 Quick Start

### Build & Install
```bash
cd C:\Users\Nakata\Documents\android-technical-test

# Full build
./gradlew build

# Install debug APK on device/emulator
./gradlew installDebug

# Run the app
./gradlew runDebug
```

---

## 📱 Manual Testing Checklist

### Test Case 1: App Launch & Tab Visibility ✅
- [ ] App launches successfully
- [ ] Bottom navigation bar appears at bottom of screen
- [ ] Two tabs visible: "Albums" (home icon) and "Favorites" (star icon)
- [ ] Albums tab is selected by default (highlighted)
- [ ] Albums list displays correctly

### Test Case 2: Tab Switching (Albums → Favorites)
- [ ] Tap Favorites tab
- [ ] Favorites screen displays immediately
- [ ] Top app bar shows "Favorites" title
- [ ] Favorites tab is now highlighted
- [ ] Albums tab becomes unselected
- [ ] No lag or stutter during transition

### Test Case 3: Empty Favorites State
- [ ] On Favorites tab with no favorites marked
- [ ] See message: "No favorites yet. Add some from Albums tab."
- [ ] Message is centered and readable
- [ ] No UI errors or crashes

### Test Case 4: Favorite Toggle (Albums → Appears in Favorites)
- [ ] On Albums tab
- [ ] Tap star icon on any album
- [ ] Star icon fills/changes color (favorite marked)
- [ ] Immediately switch to Favorites tab
- [ ] Album appears in Favorites list
- [ ] ✅ Real-time sync verified

### Test Case 5: Favorite Toggle (Favorites → Disappears)
- [ ] On Favorites tab with favorite album displayed
- [ ] Tap star icon on the favorite album
- [ ] Star icon unfills/changes color (favorite unmarked)
- [ ] Album disappears from Favorites list immediately
- [ ] ✅ Real-time removal verified

### Test Case 6: Cross-Tab Consistency
- [ ] Mark album as favorite in Albums tab
- [ ] Switch to Favorites → Album present
- [ ] Switch back to Albums → Album still marked favorite
- [ ] Unmark in Albums tab
- [ ] Switch to Favorites → Album no longer present
- [ ] ✅ Consistency verified

### Test Case 7: Multiple Favorites
- [ ] Mark 3-5 albums as favorites
- [ ] Switch to Favorites tab
- [ ] All 5 albums display in a scrollable list
- [ ] Each has star icon filled
- [ ] Spacing and layout is consistent
- [ ] ✅ List rendering works correctly

### Test Case 8: Favorite Toggle in Favorites Tab
- [ ] On Favorites tab with multiple albums
- [ ] Tap star on one album to unmark
- [ ] Album disappears from list
- [ ] Other albums remain unchanged
- [ ] Switch to Albums tab
- [ ] Previously unmarked album shows empty star
- [ ] ✅ Sync from Favorites to Albums verified

### Test Case 9: Navigation Back Stack
- [ ] On Albums tab
- [ ] Tap album to go to detail screen
- [ ] Detail screen displays album info
- [ ] Tap back button
- [ ] Returns to Albums list
- [ ] Switch to Favorites and back to Albums
- [ ] List is at top (not at detail)
- [ ] ✅ Back stack cleared on tab switch

### Test Case 10: UI Responsiveness
- [ ] Tap Albums tab multiple times quickly
- [ ] Tap Favorites tab multiple times quickly
- [ ] Toggle favorite repeatedly
- [ ] Switch tabs while loading
- [ ] No crashes or ANR (App Not Responding)
- [ ] ✅ App remains responsive

### Test Case 11: Error State (Optional - requires network issue)
- [ ] Disable network/WiFi
- [ ] Trigger data refresh in Favorites tab
- [ ] Error message displays with Retry button
- [ ] Tap Retry button
- [ ] Attempts to reload (may fail with no network)
- [ ] No crashes

### Test Case 12: Orientation Change
- [ ] On Albums tab in portrait mode
- [ ] Rotate device to landscape
- [ ] App reorients, bottom nav still visible
- [ ] Tab selection preserved
- [ ] Data still displays correctly
- [ ] Rotate back to portrait
- [ ] ✅ Orientation change handled

### Test Case 13: Persistent Favorites Across App Restart
- [ ] Mark 2-3 albums as favorites
- [ ] Close app completely
- [ ] Reopen app
- [ ] Switch to Favorites tab
- [ ] Previously marked favorites still present
- [ ] ✅ Persistence verified (via Room database)

### Test Case 14: Performance - Scrolling
- [ ] Mark 10+ albums as favorites
- [ ] Switch to Favorites tab
- [ ] Scroll up and down through list smoothly
- [ ] No frame drops or stuttering
- [ ] Toggle favorite while scrolling
- [ ] ✅ Smooth scrolling performance

### Test Case 15: Material Design Compliance
- [ ] Bottom navigation bar uses Material 3 styling
- [ ] Icons are clear and recognizable (home, star)
- [ ] Tab labels are visible and readable
- [ ] Ripple effect appears on tap
- [ ] Color scheme is consistent with app theme
- [ ] ✅ Material 3 compliance verified

---

## 📊 Expected Results Summary

| Test | Expected Result | Status |
|------|-----------------|--------|
| App launches | Bottom nav visible with 2 tabs | ✅ |
| Tab switching | Tabs switch smoothly without lag | ✅ |
| Empty favorites | "No favorites" message displays | ✅ |
| Favorite toggle (add) | Album appears in Favorites immediately | ✅ |
| Favorite toggle (remove) | Album disappears from Favorites immediately | ✅ |
| Cross-tab consistency | Changes sync across both tabs | ✅ |
| Multiple favorites | List displays all favorites with spacing | ✅ |
| Back stack | Tab switch clears back stack | ✅ |
| Responsiveness | App handles rapid taps without crashing | ✅ |
| Orientation | App reorients and preserves state | ✅ |
| Persistence | Favorites survive app restart | ✅ |
| Scrolling | Smooth scrolling with 10+ items | ✅ |
| Material Design | Proper Material 3 styling applied | ✅ |

---

## 🐛 Troubleshooting

### Issue: "No favorites yet" message doesn't appear
- **Check**: Did you properly clear all favorites first?
- **Solution**: Go to Albums tab, click favorite icons to unmark all albums

### Issue: Bottom nav not visible
- **Check**: App is running in full screen?
- **Solution**: Verify `Scaffold` wrapper in AppScreen.kt includes `bottomBar` parameter

### Issue: Favorite changes not syncing between tabs
- **Check**: Is network connectivity active? (Real-time updates via repository)
- **Solution**: Verify repository's Flow-based updates are working in AlbumsViewModel

### Issue: App crashes when switching tabs
- **Check**: Any compilation errors in build?
- **Solution**: Run `./gradlew clean build` and verify no errors

### Issue: Detail screen not accessible from Favorites tab
- **Note**: This is expected behavior (by design)
- **Details**: Favorites tab is read-only; detail view only accessible from Albums tab

---

## 📋 Test Report Template

```
Date: [Date]
Tester: [Name]
Device: [Device Model]
OS Version: [Android Version]
App Version: [Version]

✅ Passed Tests:
- Test 1: ...
- Test 2: ...

❌ Failed Tests:
- Test X: ...
  Expected: ...
  Actual: ...

⚠️ Issues Found:
- Issue 1: ...
  Severity: [High/Medium/Low]
  Steps to Reproduce: ...

Overall Status: [PASS/FAIL]
```

---

## 🎯 Automation Test Ideas (Future)

1. **Unit Tests** for FavoritesViewModel
   - Test favorite filtering logic
   - Test state updates on favorite toggle
   - Test error handling

2. **Integration Tests** for tab switching
   - Verify state sync across tabs
   - Verify back stack management
   - Verify analytics tracking

3. **UI Tests** with Compose
   - Verify bottom nav appears
   - Verify tab switching navigation
   - Verify favorite toggle UI updates

4. **Performance Tests**
   - Test scroll performance with 100+ items
   - Test favorite toggle latency
   - Test memory usage during tab switches

---

## ✅ Verification Checklist

Before declaring implementation complete:

- [ ] Build completes with 0 errors: `./gradlew build`
- [ ] Lint passes: `./gradlew lint`
- [ ] App launches without crashes
- [ ] Bottom nav bar visible with 2 tabs
- [ ] Tab switching works smoothly
- [ ] Favorite toggle works in both tabs
- [ ] Real-time sync verified across tabs
- [ ] Empty state displays correctly
- [ ] Error handling works (retry button)
- [ ] Material 3 styling applied correctly
- [ ] Analytics events tracked
- [ ] No ANRs during rapid interactions
- [ ] Orientation changes handled
- [ ] Favorites persist across app restart

**All checked = Ready for production! 🚀**

---

## 📞 Support & Questions

For any issues during testing:
1. Check the Troubleshooting section above
2. Review the BOTTOM_NAV_IMPLEMENTATION_COMPLETE.md for detailed architecture
3. Inspect the code files for implementation details
4. Run `./gradlew clean build` to rebuild from scratch

---

## 🎉 Success Criteria

✅ **All 15 test cases pass**
✅ **Zero crashes or ANRs**
✅ **Smooth tab switching**
✅ **Real-time favorite sync**
✅ **Material 3 compliance**
✅ **Offline persistence works**

**When all criteria met, implementation is COMPLETE and ready for production!** 🚀
