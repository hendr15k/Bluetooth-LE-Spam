# Autoresearch Configuration

## Goal
Add new BLE spam device targets for modern devices (2024-2025)

## Metric
- **Name**: device_count (number of new working device entries added)
- **Direction**: higher is better
- **Extract command**: grep -c "to \"" app/src/main/java/de/simon/dankelmann/bluetoothlespam/AdvertisementSetGenerators/*.kt

## Target Files
- AdvertisementSetGenerators/*.kt (add new device entries, new generators)
- Models/*.kt (if needed for new protocols)
- Constants/*.kt (protocol constants)

## Read-Only Files
- app/build.gradle (build config)
- Services/*.kt (BLE service implementation)
- MainActivity.kt (UI entry points)

## Run Command
```bash
./gradlew assembleDebug --no-daemon 2>&1 | tee build.log
```

## Time Budget
- **Per experiment**: 5 minutes (build time)
- **Kill timeout**: 10 minutes

## Constraints
- Don't break existing generators
- Maintain Kotlin code style
- Keep manufacturer IDs correct
- Only add proven/documented device IDs

## Branch
autoresearch/new-ble-targets

## Notes
Current targets already in app:
- Apple: Vision Pro, Watch, AirPods, iOS17 Crash (patched in iOS18)
- Google Fast Pair: 500+ device IDs (patched on modern Android)
- Samsung Easy Setup: Buds, Watch
- Microsoft Swift Pair
- Lovespouse

New targets to research:
- Xiaomi/Huawei pairing protocols
- Updated Fast Pair IDs for 2024-2025 devices
- BLE Audio/Auracast
- More Samsung variants
- Google Pixel newer models