# Contributing

Thanks for wanting to help. This is a small library with a deliberately small surface, so most contributions are small too: a native fix, a new method, a doc correction.

## Local setup

```bash
nvm use            # Node version is pinned in .nvmrc (v18)
npm install
npm run build
```

`npm run build` runs three things, in this order:

1. `build:babel`, which emits CommonJS into `lib/commonjs` and ESM into `lib/module` from `src/`, each followed by its declaration build (`tsconfig.commonjs.json`, `tsconfig.module.json`, output under `lib/typescript/`).
2. `build:types`, the declaration-only pass into `lib/types`.
3. `codegen`, which runs the React Native codegen and then `node utils/patchCodegen.mjs`. That patch step exists because codegen emits the generated Java specs under `com.facebook.fbreact.specs` instead of the `javaPackageName` from `codegenConfig` (facebook/react-native#45079), so it rewrites the package declaration and moves the files to `com.reloadwidget`. If you change `codegenConfig`, check that script still lines up.

There is no test suite and no CI in this repository, so verification is manual: link your branch into a real React Native app that already has a widget, place that widget on a home screen, and watch whether the calls actually refresh it.

## What lives where

| Path | What it is |
| ----------- | ----------- |
| `src/index.tsx` | the public `reloadAllWidgets`, `reloadWidget` and `getInstalledWidgets` functions, the `InstalledWidget` type, the new/old architecture selection, the defensive wrapper that turns a failure into `false` / `[]`, and the linking-error proxy |
| `src/NativeReloadWidget.ts` | the TurboModule spec, the input to codegen |
| `ios/ReloadWidget.h` / `.mm` | the iOS module, `RCT_EXPORT_METHOD` implementations serving both architectures behind an `RCT_NEW_ARCH_ENABLED` switch |
| `ios/ReloadWidgetImpl.swift` | the actual WidgetKit calls, which cannot live in the `.mm`: `WidgetCenter` is not exposed to Objective-C at all, so the Objective-C++ file is a shim over Swift |
| `android/src/main/.../ReloadWidgetModuleImpl.kt` | the actual Android work (provider enumeration and the `ACTION_APPWIDGET_UPDATE` broadcast), shared by both architectures |
| `android/src/newarch`, `android/src/oldarch` | thin per-architecture wrappers, selected by the Gradle build |
| `utils/patchCodegen.mjs` | the codegen package-name workaround described above |

If you touch the JS signature, `src/index.tsx` and `src/NativeReloadWidget.ts` have to stay in agreement, and the spec change has to be carried into both native implementations. Keep new Android logic in `ReloadWidgetModuleImpl` so the two wrappers stay thin and cannot drift apart, and keep new WidgetKit calls in the Swift core rather than reaching for them from the shim, where they are not visible.

Two behaviors are contracts rather than implementation details, so please preserve them: **nothing rejects** (a failure is logged with `console.error` and resolved as `false` or `[]`), and every WidgetKit call stays `@available`-guarded so a deployment target below iOS 14 still compiles.

## Both architectures are supported, so check both

The support table in the README claims the new architecture and the old one, on iOS and on Android. Any change to the native surface should be checked against each combination it could plausibly break, not just the one your test app happens to use:

- **iOS**: `RCT_NEW_ARCH_ENABLED=1 pod install` for the new architecture, and a plain `pod install` for the old one. The header compiles a different protocol conformance in each case, so a change that builds under one can fail under the other.
- **Android**: flip `newArchEnabled` in the host app's `gradle.properties`. The Gradle build adds `src/newarch` or `src/oldarch` to the source set from that flag, so only one of the two wrappers is compiled per build.

Rebuild the app rather than reloading JS: nothing native reaches the running app through Metro. And test on a real device where you can: widgets are not available on the iOS Simulator in every configuration, and there is no reliable way to force an immediate redraw.

In your pull request, say which combinations you actually ran and which you could not. An honest "not tested on Android old arch" is far more useful than silence.

## Commits

We follow [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/), and commit messages are in English for consistency. The existing history uses `type(scope): subject`, for example:

```
feat(android): reload a single provider by class name
fix(ios): guard the WidgetKit calls below iOS 14
docs(readme): document the per-platform reloadWidget contract
```

Work on a branch off `main`, keep a pull request to one concern, and mention any behavior change in the description so it can land in `CHANGELOG.md`.

## Reporting a bug

Open an issue using the bug report template. Beyond the reproduction steps, a widget bug is only actionable with all of this:

- **Platform and OS version**, for example iOS 17.4 or Android 14.
- **React Native version**, and the output of `react-native info` (the template asks for it).
- **Architecture**: new or old. On Android this is `newArchEnabled`, on iOS it is whether pods were installed with `RCT_NEW_ARCH_ENABLED=1`.
- **Real device or simulator/emulator**, because widgets behave differently there and a reload that appears to do nothing on a simulator often means nothing at all.
- **Which widget**, and how it is placed: the WidgetKit `kind` (or the Android provider class name), the family or size the user placed, and how many copies are on the home screen. `getInstalledWidgets()` prints most of that for you, so pasting its output is the fastest way to answer this.
- **Which call you made and what it returned**, verbatim, and what you expected instead. A `false` and an empty array both mean either a real answer or a swallowed failure, so say what the console showed as well.
- **What the widget reads, and when.** If it reads from a server or a shared store, say whether the write had been confirmed before you called the reload: a reload fired against data that has not landed yet rebuilds the widget with the old content, which looks exactly like a reload that did nothing.
- **Anything logged to the console.** Failures are logged with `console.error` rather than thrown, so the underlying error is in the log and not in your `catch`.

For a feature request, the feature request template is the right place, and it helps to say which call you need and what you are building.

## Security

Vulnerabilities have their own policy in [SECURITY.md](SECURITY.md), including which versions are covered and what to expect after a report. Read that before filing one.
