# react-native-reload-widget

<p>
	<a href="https://github.com/wneel/react-native-reload-widget/blob/HEAD/LICENSE">
		<img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="react-native-reload-widget is released under the MIT license." />
	</a>
	<a href="https://www.npmjs.com/package/react-native-reload-widget">
		<img src="https://img.shields.io/npm/v/react-native-reload-widget?color=brightgreen&label=npm%20package" alt="Current npm package version." />
	</a>
	<a href="https://www.npmjs.com/package/react-native-reload-widget">
		<img src="https://img.shields.io/npm/dm/react-native-reload-widget" alt="Number of downloads per month." />
	</a>
	<a href="https://github.com/wneel/react-native-reload-widget/blob/HEAD/package.json">
		<img src="https://img.shields.io/badge/runtime%20dependencies-0-brightgreen" alt="Zero runtime dependencies." />
	</a>
</p>

**Your app just changed. The home-screen widget beside it still shows the old state.**

iOS WidgetKit and Android AppWidget both redraw on a schedule of their own, on the order of fifteen minutes and entirely at the system's discretion. So after a language change, a login, or a data write, the widget keeps displaying what it rendered last time until the OS gets round to it. React Native exposes no way to say "that data is stale, rebuild now": the calls that do it (`WidgetCenter` on iOS, an `ACTION_APPWIDGET_UPDATE` broadcast on Android) exist in native code only.

`react-native-reload-widget` is that one line. Reload every widget, reload one kind, or list what the user actually has placed on their home screen. A TurboModule on the new architecture, a bridge module on the old one, iOS and Android, no runtime dependencies.

<br />

## 🔁 APIs this replaces

There is no JavaScript API here to replace, which is the point. Without this library you have two options:

- **Hand-write a native module per platform.** A Swift file for `WidgetCenter` (which, as the [How it works](#-how-it-works) section explains, cannot be reached from Objective-C at all), a Kotlin file that builds the broadcast intent and resolves the placed widget ids, an Objective-C++ shim on the iOS side, and the new/old architecture wiring on both. That is a few hundred lines of native code in an app whose feature was "refresh the widget after login".
- **Wait out the OS refresh cycle.** Free, and it means the widget is wrong for up to a quarter of an hour immediately after the user did the thing that changed it, which is exactly when they look at it.

What that becomes here:

```diff
- // ios/WidgetReloader.swift      WidgetCenter.shared.reloadAllTimelines()
- // ios/WidgetReloader.mm         an Objective-C++ shim, because WidgetCenter is Swift-only
- // android/.../WidgetReloader.kt AppWidgetManager + an explicit ACTION_APPWIDGET_UPDATE broadcast
- // plus the TurboModule spec, the old-arch fallback, and the bridging on both sides
+ import { reloadAllWidgets } from 'react-native-reload-widget';
+
+ await reloadAllWidgets();
```

No platform branch in your code, no native target to maintain, and the same call on both.

<br />

## ✨ Features
- Reload every widget the app owns with `reloadAllWidgets()`, after a language change, a login, or any write the widget reads.
- Reload a single widget kind with `reloadWidget(kind)`, so a change that only affects one widget does not rebuild the others.
- List what the user has actually placed with `getInstalledWidgets()`: one entry per placed instance, with its kind, its family on iOS and its id on Android.
- Compatible with React Native >= 0.76, the new architecture (TurboModules) and backward compatibility with old arch.
- Minimal setup with low resource usage. No permission, no entitlement, no `Info.plist` key.
- No call ever rejects: a failure resolves `false` (or `[]`) and is logged, so a widget refresh cannot take down the screen that awaited it.
- TypeScript types shipped with the package. Zero runtime dependencies.

| Support |  |
| ----------- | -----------: |
| react-native version      | >=0.76 |
| Android   | ✅ |
| iOS   | ✅ |
| New Architecture   | ✅ |
| Old Architecture   | ✅ |
| TypeScript types   | ✅ |
| Runtime dependencies   | 0 |

Android `minSdkVersion` defaults to 21 and follows your app's `rootProject.ext` when it sets one. On iOS the podspec uses React Native's own `min_ios_version_supported`, so the minimum tracks your React Native version rather than being pinned here. WidgetKit itself needs iOS 14, and every call into it is `@available`-guarded, so a project with a lower deployment target still compiles and the three methods simply resolve `false` / `[]` there.

<br />

## 📦 Installation
```bash
npm install react-native-reload-widget
```

## Additional Steps (iOS only)
After installation, run the following command in your project's ios directory:
```bash
cd ios && pod install
```

Then rebuild the app. Autolinking handles the rest on both platforms: the package ships its podspec and its `react-native.config.js`, so there is nothing to register by hand.

**No entitlement, no capability and no `Info.plist` key is required.** The reload API asks the system to rebuild timelines your own app already owns, so there is no permission involved and nothing to declare. If you already share data with your widget through an App Group, that App Group is for the data, not for this module, and it stays exactly as it was.

One placement rule, and it matters: **add this pod to the main app target only, never to the widget extension.** See [Limitations](#-limitations) for why.

<br />

## 🧪 Expo

- **There is no Expo config plugin in this package**, and it does not need one. A plugin exists to patch native config at prebuild time, and this module adds nothing to patch: its `AndroidManifest.xml` declares no permissions and it requires no `Info.plist` entry. Autolinking is the whole integration. The widget target itself is still your own native work, prebuild or otherwise, and no plugin here would change that.
- **It cannot run in Expo Go**, because Expo Go ships a fixed set of native modules. The library's own linking error says so out loud.
- In an Expo project, use a development build: `npx expo prebuild` then `npx expo run:ios` / `npx expo run:android`, or EAS Build. From there it behaves like any other autolinked native module.

<br />

## 📖 Usage Example
The common case is one line after the thing that made the widget stale:
```tsx
import { reloadAllWidgets } from 'react-native-reload-widget';

await saveLanguagePreference('fr');
await reloadAllWidgets();
```

If only one widget reads what changed, name it and leave the others alone:
```tsx
import { reloadWidget } from 'react-native-reload-widget';

const reloaded = await reloadWidget('RadarWidget');
// true once the request reached the system, false when it could not be delivered
```

And if you want to know what the user actually has on their home screen, for instance to skip the work entirely when they have no widget placed:
```tsx
import { getInstalledWidgets } from 'react-native-reload-widget';

const widgets = await getInstalledWidgets();
// iOS:     [{ kind: 'RadarWidget', family: 'systemSmall', id: null },
//           { kind: 'RadarWidget', family: 'systemMedium', id: null }]
// Android: [{ kind: 'RadarWidgetProvider', family: null, id: '42' }]

if (widgets.length > 0) {
	await reloadAllWidgets();
}
```

### API

#### `reloadAllWidgets`

```ts
function reloadAllWidgets(): Promise<boolean>
```

| | |
| ----------- | ----------- |
| returns | A promise resolving to `true` when the reload was requested, `false` when it could not be (no widget support on this OS version, or the request failed). |

Asks the system to rebuild every widget the app owns. This is the call you want after a language change, a sign-in or a sign-out, or any write whose result the widget displays.

It is async, and it never rejects. Any failure inside the call is caught, logged with `console.error`, and `false` is resolved instead. A `true` means the request reached the platform, not that pixels have changed yet: see [Limitations](#-limitations).

#### `reloadWidget`

```ts
function reloadWidget(kind: string): Promise<boolean>
```

| | |
| ----------- | ----------- |
| `kind` | The widget to reload. On iOS this is the WidgetKit `kind` string, the one you pass to `StaticConfiguration(kind:provider:)`. On Android it is the provider's simple class name, for example `RadarWidgetProvider`. |
| returns | A promise resolving to `true` when the reload request reached the system, `false` when it could not be delivered. |

**What an unknown kind resolves to depends on the platform**, because the two platforms know different things:

- **Android** enumerates the app's own AppWidget providers before sending anything, so it genuinely knows whether the kind exists. No provider matches the kind, and it resolves `false`.
- **iOS** cannot know. WidgetKit's `reloadTimelines(ofKind:)` returns nothing and is a silent no-op for an unknown kind, so the call itself tells you nothing. iOS therefore resolves `true` whenever the request was dispatched, which is any iOS 14 and above, even when no widget of that kind is currently placed. It resolves `false` only when WidgetKit is unavailable, meaning below iOS 14.

That asymmetry is a real platform difference rather than a bug: the information simply exists on one side and not on the other. If you need to know what the user has actually placed, call [`getInstalledWidgets`](#getinstalledwidgets) and read the answer there.

Like the other two, it is async and never rejects: failures are logged with `console.error` and `false` is resolved.

#### `getInstalledWidgets`

```ts
type InstalledWidget = {
	kind: string;
	family: string | null;
	id: string | null;
};

function getInstalledWidgets(): Promise<InstalledWidget[]>
```

| | |
| ----------- | ----------- |
| returns | A promise resolving to one entry **per placed widget instance**, or `[]` when the user has placed none (or the lookup failed). |

**One entry per placed widget, not one per kind.** A user who has dragged two copies of the same widget onto their home screen, say a small one and a medium one, gives you two entries with the same `kind`. Count the array if you want to know how many widgets are out there; deduplicate on `kind` if you want to know which widgets are in use.

| Field | |
| ----------- | ----------- |
| `kind` | On iOS, the WidgetKit kind string. On Android, the provider's simple class name. Never `null`. |
| `family` | On iOS, the widget family the user placed: `"systemSmall"`, `"systemMedium"`, `"systemLarge"`, `"accessoryCircular"` and so on. On Android, always `null`: the platform has no equivalent notion, widgets there are sized in cells rather than by family. |
| `id` | On Android, the `appWidgetId` of that placed instance, as a string. On iOS, always `null`: WidgetKit gives placed widgets no stable per-instance identifier of that shape. |

Like the other two, it is async and never rejects: failures are logged with `console.error` and `[]` is resolved. An empty array therefore means either "nothing placed" or "the lookup failed", and the console tells you which.

<details>

<summary><h2>Minimal App demo:</h2></summary>

```tsx
import { useState } from 'react';
import { View, Text, Button } from 'react-native';
import { reloadAllWidgets, getInstalledWidgets } from 'react-native-reload-widget';

export default function App() {
	const [status, setStatus] = useState<string>('idle');

	const refresh = async () => {
		const widgets = await getInstalledWidgets();
		const ok = await reloadAllWidgets();
		setStatus(`${widgets.length} placed, reload requested: ${ok}`);
	};

	return (
		<View style={{ flex: 1, alignItems: 'center', justifyContent: 'center' }}>
			<Text>{status}</Text>
			<Button title="Reload widgets" onPress={refresh} />
		</View>
	);
}
```
</details>

<br />

## 🔍 How it works

Each call is a native call per platform, resolved straight to the promise. Nothing is cached, polled or stored, and no timer of any kind is installed.

| Method | iOS | Android |
| ----------- | ----------- | ----------- |
| `reloadAllWidgets` | `WidgetCenter.shared.reloadAllTimelines()` | an explicit `ACTION_APPWIDGET_UPDATE` broadcast to every provider of the app |
| `reloadWidget` | `WidgetCenter.shared.reloadTimelines(ofKind:)` | the same broadcast, sent only to the provider whose simple class name matches |
| `getInstalledWidgets` | `WidgetCenter.shared.getCurrentConfigurations()` | `AppWidgetManager.getInstance(ctx).installedProviders`, filtered to the app's own package |

**On iOS**, all three are WidgetKit, which means iOS 14 and above. Every call sits behind an `@available` guard, so a project whose deployment target is lower still compiles cleanly and gets `false` / `[]` back at runtime rather than a crash.

There is an implementation detail here worth knowing, because it explains the shape of the iOS folder: **`WidgetCenter` is not exposed to Objective-C at all.** The WidgetKit umbrella header is essentially empty, and the API exists only in the Swift interface, so no amount of importing gets you `WidgetCenter` from a `.m` or `.mm` file. The package therefore ships a small Swift core holding the actual WidgetKit calls, behind an Objective-C++ TurboModule shim that React Native can see. That split is not a style preference, it is the only way to reach the API from a React Native module.

**On Android**, the reload is a broadcast. The module enumerates `AppWidgetManager.getInstance(ctx).installedProviders` and keeps the providers whose package matches the app's own, then sends each one an explicit `ACTION_APPWIDGET_UPDATE` intent carrying the ids of its placed instances. Enumerating and filtering is deliberate rather than lazy: `getInstalledProvidersForPackage` does exactly this in one call, but it is API 26 and above, and this package supports further back than that.

Both architectures are wired to the same calls, which is why the support table claims both:

- **JS**: `src/index.tsx` uses the TurboModule spec (`src/NativeReloadWidget.ts`) when the TurboModule proxy is present, and `NativeModules.ReloadWidget` otherwise. The defensive wrapper that turns a failure into `false` / `[]` plus a `console.error` lives here, once, for both paths.
- **iOS**: the header switches on `RCT_NEW_ARCH_ENABLED`, conforming to the generated `NativeReloadWidgetSpec` on the new architecture and to `RCTBridgeModule` on the old one. The `RCT_EXPORT_METHOD` implementations serve both, and both call into the same Swift core.
- **Android**: the Gradle build compiles `src/newarch` or `src/oldarch` depending on your app's `newArchEnabled`. Both are thin wrappers over the same `ReloadWidgetModuleImpl`, so the two paths cannot drift.

<br />

## ⚖️ Compared to react-native-widget-center

[`react-native-widget-center`](https://www.npmjs.com/package/react-native-widget-center) is the closest existing package, and it solves the same core problem: it wraps `WidgetCenter` so JavaScript can ask iOS to reload timelines. If your app is iOS-only, already on the old architecture, and already using it happily, there is nothing wrong with it and no urgency to move.

The difference is scope and age. It is an older, iOS-focused package written before the new architecture landed, so it has not kept pace with TurboModules, and it has nothing to say about Android, where the equivalent call is a broadcast rather than a framework method.

| | react-native-reload-widget | react-native-widget-center |
| ----------- | ----------- | ----------- |
| Platforms | iOS, Android | iOS |
| New Architecture | TurboModule | not supported |
| Old Architecture | bridge module fallback | yes |
| Reload all | `reloadAllWidgets()` | yes |
| Reload one kind | `reloadWidget(kind)` | yes |
| Enumerate placed widgets | `getInstalledWidgets()` | no |
| Failure behavior | resolves `false` / `[]`, logs | rejects |

**Pick `react-native-widget-center`** if you are iOS-only, staying on the old architecture, and it already works in your app.

**Pick this one** if you need Android as well, if you are on (or heading for) the new architecture, if you want to know what the user has actually placed before doing work for it, or if you would rather a stale-widget refresh could never reject into a screen that awaited it.

<br />

## 🚧 Limitations

Worth knowing before you install, and none of these are going away:

- **On Android, the reload is a broadcast, so `true` means "dispatched", not "redrawn".** The promise resolves once the `ACTION_APPWIDGET_UPDATE` intent has been sent. The provider's `onUpdate` runs afterwards, on the system's schedule, and whatever it does next (a network call, a database read) takes however long it takes. Do not treat the resolved value as a signal that the user is now looking at fresh pixels.
- **A reload request is a hint, not a guarantee.** WidgetKit coalesces rapid successive requests, so calling this in a tight loop will not give you one redraw per call: in practice a burst collapses into far fewer actual rebuilds. Treat reloads as something you spend sparingly, at the moments the data genuinely changed, and confirm the behavior you get on a real device rather than assuming a particular budget. Neither platform documents a number you could code against, and this package invents none.
- **A reload only helps once the data it reads is actually there.** If the widget reads from a server, a shared container or an App Group store, firing a reload synchronously with an optimistic local write rebuilds the widget against data that has not landed yet, and you get a fresh render of the old state. Reload *after* the write is confirmed, not alongside it. This is the single most common way a correct reload call still shows stale content.
- **On iOS, link this library into the main app target only, never into the widget extension.** The extension does not need it: it rebuilds itself when the system tells it to, and asking it to reload itself is not a thing. Adding native dependencies to an app extension widens its dependency surface and can force `APPLICATION_EXTENSION_API_ONLY` constraints onto pods that then have to satisfy them, which is a build failure that looks nothing like its cause.
- **Widgets are awkward to test.** They are not available on the iOS Simulator in every configuration, and there is no reliable way to force an immediate redraw for verification, so a reload that appears to do nothing may simply be a system that has not scheduled the rebuild yet. Test on a real device, and give it a moment.
- **Not compatible with Expo Go.** It is a native module, so it needs a development build or the bare workflow. See the [Expo section](#-expo).
- **It reloads widgets, it does not build them.** Creating the widget, its timeline provider and its data sharing is your own native work on both platforms. This package assumes the widget already exists and only handles the "refresh it now" call.
- **iOS and Android only.** Those are the two native folders in the package.

<br />

## 🔄 Roadmap and Future Features

Nothing is committed. The three methods above are the whole intended surface, and the Limitations are scope rather than a backlog: widget creation, data sharing and timeline policy belong to your own native targets, and a JavaScript package is a poor place to put them.

Ideas that would fit, if someone needs them enough to open an issue:

- Reloading several kinds in one call, rather than one `reloadWidget` per kind.
- Surfacing the iOS reload budget signals, if a stable way to read them appears.
- Live Activities, which share WidgetKit but have their own lifecycle and would be a different package rather than a method here.

Issues and pull requests are welcome either way.

<br />

## 🏭 Used in production

This module ships in [Animalert](https://animalert.app), a live lost-pet reporting platform available in several languages, on the [App Store](https://apps.apple.com/app/id6480419312) and [Google Play](https://play.google.com/store/apps/details?id=com.animalert). Its home-screen widget shows nearby reports in the user's own language, and both of those change from inside the app: the language when the user picks one in settings, the reports as they are created and resolved. Before this module, the widget kept the previous language, and the previous list, until the OS refreshed it on its own. Now the app asks.

<br />

## 🛠️ Contributing
Contributions are welcome! We follow the [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/) guidelines. To contribute:

1. Fork the repo.
2. Clone it and create a new branch.
3. Follow the commit message conventions.
4. Open a Pull Request!
> 💡 Tip: Make sure your commit messages are in English for consistency!

See [CONTRIBUTING.md](CONTRIBUTING.md) for local setup, the two-architecture check, and what a useful bug report contains. Released versions are listed in [CHANGELOG.md](CHANGELOG.md).

<br />

## 📞 Support
If you have questions or issues, feel free to open an issue on GitHub. I'll stay active to respond to queries and provide support.

Security reports have their own path: see [SECURITY.md](SECURITY.md).

<br />

## 📄 License

MIT. See [LICENSE](LICENSE).
