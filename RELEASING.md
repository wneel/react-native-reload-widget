# Releasing

Maintainer runbook. It follows the same ritual as `react-native-get-device-locale`: `npm version` writes the commit and the tag, `npm publish` rebuilds from a clean tree on its own, and the tag is pushed last.

This file is not shipped to npm. The `files` array in `package.json` lists what goes in the tarball and this is not on it, so it lives on GitHub only.

<br />

## What npm does on its own

`prepare` is wired to `preprepare` (`npm run clean`) followed by `npm run build`, and npm runs `prepare` before both `npm pack` and `npm publish`. So every publish deletes `lib/`, `ios/generated/` and `android/generated/`, then rebuilds Babel CJS, Babel ESM, the two declaration passes and codegen from scratch.

Two consequences:

- You never have to build by hand before publishing. You do have to make sure the build would succeed, because a failing `prepare` aborts the publish.
- `node_modules/` must be installed in this folder, since the build runs Babel, tsc and the community CLI from it. A publish from a freshly cloned tree with no `npm install` fails.

<br />

## One time, before the first publish

1. **Confirm the name is still free.**

	```bash
	npm view react-native-reload-widget version
	```

	A `404 Not Found` is the answer you want. Verified free on 2026-09-13.

2. **Make it a git repository.** It is not one yet, unlike the other two libraries.

	```bash
	cd ../react-native-reload-widget
	git init -b main
	git add .
	git commit -m "feat: reload home screen widgets from JavaScript on iOS and Android"
	```

	`.gitignore` already keeps `node_modules/`, `lib/`, both `generated/` folders and `*.tgz` out.

3. **Create the GitHub repository and push.** The `repository`, `bugs` and `homepage` fields in `package.json` already point at `github.com/wneel/react-native-reload-widget`, so the name has to match exactly.

	```bash
	gh repo create wneel/react-native-reload-widget --public --source=. --remote=origin --push
	```

	`gh` is already authenticated here as `wneel` with the `repo` scope.

4. **Log in to npm.** The registry currently answers `401` on this machine, so there is no active session.

	```bash
	npm login
	```

	`publishConfig` already sets `access: public` and the public registry, so no flags are needed at publish time.

<br />

## Every release

1. **Check the tree is what you want to ship.**

	```bash
	git status
	npm run build
	```

	The build is repeated at publish time anyway, but running it here means a failure costs you nothing.

2. **Update `CHANGELOG.md`.** Move whatever sits under `## [Unreleased]` into a new version heading and leave `Nothing pending.` behind. The file states its own rule at the top: dates are the npm publish dates, so use the day you actually publish, not the day you wrote the code.

3. **Bump the version.** Skip this for 1.0.0, which `package.json` already carries.

	```bash
	npm version patch   # or minor, or major
	```

	This writes the version, commits it and creates the `vX.Y.Z` tag, which is where the `v1.0.0` through `v1.1.0` tags on `get-device-locale` came from. For 1.0.0, tag by hand instead:

	```bash
	git tag v1.0.0
	```

4. **Dry run, and read the file list.**

	```bash
	npm publish --dry-run
	```

	Expect 38 files: `src/`, `lib/` (CJS, ESM and both declaration trees), `ios/` including `ios/generated/`, `android/` including `android/generated/`, the podspec, `react-native.config.js`, `README.md`, `CHANGELOG.md` and `LICENSE`. No dotfiles, no `package-lock.json`, no tarball, no `node_modules`.

	Both `generated/` folders have to be in there. `codegenConfig.includesGeneratedCode` is `true`, which tells consuming apps not to run codegen themselves, so a tarball missing them does not build anywhere.

5. **Publish.**

	```bash
	npm publish
	```

	Have your 2FA device ready if the account asks for an OTP.

6. **Push the code and the tag.**

	```bash
	git push --follow-tags
	```

7. **Check what landed.**

	```bash
	npm view react-native-reload-widget version
	```

<br />

## After publishing: point Animalert at the registry

The app currently installs the library from a local tarball:

```json
"react-native-reload-widget": "file:../react-native-reload-widget/react-native-reload-widget-1.0.0.tgz",
```

Once the package is on npm, swap it for a normal range and reinstall:

```bash
cd ../Animalert-v2
npm install react-native-reload-widget@^1.0.0
cd ios && pod install
```

One trap worth remembering, because it cost time during development: npm caches a tarball by name and version, so reinstalling the same version number does not necessarily refresh `node_modules`. If the installed copy looks stale, delete it first:

```bash
rm -rf node_modules/react-native-reload-widget && npm install
```

<br />

## Before you publish, one call to make

`README.md` has a "Used in production" section saying the module ships in Animalert. It is wired into the app and verified working, but no App Store or Play release carrying it has gone out yet. Either publish the library after that app release, or soften the wording until it has.
