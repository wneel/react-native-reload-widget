# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Dates are the npm publish dates.

## [Unreleased]

Nothing pending.

## [1.0.0] - 2026-09-12

First release. Three methods, two platforms, both architectures.

### Added
- `reloadAllWidgets(): Promise<boolean>`, asking the system to rebuild every widget the app owns. Resolves `true` once the request reached the platform. On iOS it is `WidgetCenter.shared.reloadAllTimelines()`; on Android it is an explicit `ACTION_APPWIDGET_UPDATE` broadcast sent to each of the app's own providers with the ids of their placed instances.
- `reloadWidget(kind: string): Promise<boolean>`, reloading a single widget kind so a change affecting one widget does not rebuild the others. `kind` is the WidgetKit kind string on iOS and the provider's simple class name on Android. It resolves `true` when the request reached the system and `false` when it could not be delivered, and those two answers carry different information per platform: Android enumerates its own providers first, so an unknown kind resolves `false`, while iOS cannot tell (`reloadTimelines(ofKind:)` returns nothing and is a silent no-op for an unknown kind) and therefore resolves `true` for any dispatched request on iOS 14 and above, `false` only when WidgetKit is unavailable.
- `getInstalledWidgets(): Promise<InstalledWidget[]>`, returning one entry per **placed widget instance** rather than one per kind, so a user with two copies of the same widget on their home screen yields two entries. On iOS it reads `WidgetCenter.shared.getCurrentConfigurations()`; on Android it enumerates `AppWidgetManager.getInstance(ctx).installedProviders` filtered to the app's own package, which is used deliberately instead of `getInstalledProvidersForPackage` because that one is API 26 and above.
- The exported `InstalledWidget` type: `kind` (the WidgetKit kind on iOS, the provider's simple class name on Android), `family` (the placed widget family on iOS, such as `systemSmall` or `accessoryCircular`, and always `null` on Android, which has no equivalent notion), and `id` (the `appWidgetId` as a string on Android, always `null` on iOS).
- A shared failure contract across all three: none of them ever rejects. A failure is caught, logged with `console.error`, and resolved as `false` (or `[]` for `getInstalledWidgets`) instead, so a widget refresh cannot take down the screen that awaited it.
- New Architecture support through a TurboModule, with an old-architecture fallback. The JS entry point selects the generated spec when the TurboModule proxy is present and `NativeModules.ReloadWidget` otherwise; on Android the Gradle build compiles `src/newarch` or `src/oldarch` from the host app's `newArchEnabled`, both thin wrappers over the same implementation class, so the two paths cannot drift.
- iOS support, iOS 14 and above. `WidgetCenter` is not exposed to Objective-C at all (the WidgetKit umbrella header is essentially empty and the API exists only in the Swift interface), so the package ships a small Swift core holding the WidgetKit calls behind an Objective-C++ TurboModule shim. Every call into WidgetKit is `@available`-guarded, so a project with a lower deployment target still compiles and the three methods resolve `false` / `[]` there.
- Android support. No permission is declared and none is needed, which is also why the package requires no Expo config plugin.
- TypeScript types shipped with the package, MIT license, peer dependency `react-native >= 0.68`, and no runtime dependencies.

[Unreleased]: https://github.com/wneel/react-native-reload-widget/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/wneel/react-native-reload-widget/releases/tag/v1.0.0
