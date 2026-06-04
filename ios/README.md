# Lucky Money iOS

Native SwiftUI iOS app for the Lucky Money / Túi thần tài expense capture product.

## Requirements

- Xcode 15.4+ or newer
- iOS 17.0+
- Apple Developer account only when installing on iPhone/TestFlight

## Build

```bash
xcodebuild -project ios/LuckyMoney.xcodeproj \
  -scheme LuckyMoney \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## TestFlight

The repository includes a disabled/template workflow at `.github/workflows/ios-testflight-template.yml`.
Real TestFlight upload requires Apple Developer membership and App Store Connect signing secrets.
