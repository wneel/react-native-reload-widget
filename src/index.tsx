import { NativeModules, Platform } from 'react-native';

import type { InstalledWidget } from './NativeReloadWidget';

const LINKING_ERROR =
	`The package 'react-native-reload-widget' doesn't seem to be linked. Make sure: \n\n` +
	Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
	'- You rebuilt the app after installing the package\n' +
	'- You are not using Expo Go\n';

// @ts-ignore
const isTurboModuleEnabled = global.__turboModuleProxy != null;

const ReloadWidgetModule = isTurboModuleEnabled
	? require('./NativeReloadWidget').default
	: NativeModules.ReloadWidget;

const ReloadWidget = ReloadWidgetModule
	? ReloadWidgetModule
	: new Proxy(
			{},
			{
				get() {
					throw new Error(LINKING_ERROR);
				},
			}
		);

/**
 * Asks the system to rebuild every widget this app owns.
 *
 * Resolves false rather than throwing: a widget refresh is a nice-to-have on
 * top of whatever the caller was actually doing, so a failure here should
 * never take down the surrounding flow.
 */
export async function reloadAllWidgets(): Promise<boolean> {
	try {
		const result = await ReloadWidget.reloadAllWidgets();
		return (result);
	} catch (error) {
		console.error(error);
		return (false);
	}
}

/**
 * Asks the system to rebuild a single widget.
 *
 * `kind` is the WidgetKit kind string on iOS, and the AppWidgetProvider's
 * class name on Android (the simple name or the fully qualified one).
 *
 * Resolves false when no widget of that kind exists. That is not an error:
 * the same call is expected to hit on one platform and miss on the other.
 */
export async function reloadWidget(kind: string): Promise<boolean> {
	try {
		const result = await ReloadWidget.reloadWidget(kind);
		return (result);
	} catch (error) {
		console.error(error);
		return (false);
	}
}

/**
 * Lists the widgets currently placed on the home screen.
 *
 * One entry per placed instance, not per kind: two copies of the same widget
 * are two entries. Useful to skip the reload entirely when nothing is placed,
 * and to find out which `kind` strings are worth passing to reloadWidget.
 */
export async function getInstalledWidgets(): Promise<InstalledWidget[]> {
	try {
		const result = await ReloadWidget.getInstalledWidgets();
		return (result);
	} catch (error) {
		console.error(error);
		return ([]);
	}
}

export type { InstalledWidget };
