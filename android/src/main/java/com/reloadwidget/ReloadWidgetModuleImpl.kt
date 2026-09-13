package com.reloadwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise

class ReloadWidgetModuleImpl(private val context: Context) {

	fun reloadAllWidgets(promise: Promise) {
		try {
			val appWidgetManager = AppWidgetManager.getInstance(context)
			for (info in ourProviders(appWidgetManager)) {
				val ids = appWidgetManager.getAppWidgetIds(info.provider)
				if (ids.isNotEmpty()) {
					sendUpdateBroadcast(info.provider.className, ids)
				}
			}
			promise.resolve(true)
		} catch (e: Exception) {
			promise.reject("reload_all_widgets_failed", e.message, e)
		}
	}

	fun reloadWidget(kind: String, promise: Promise) {
		try {
			val appWidgetManager = AppWidgetManager.getInstance(context)
			val matching = ourProviders(appWidgetManager).filter { matchesKind(it, kind) }
			// An unknown kind is not an error: the widget may simply not exist on this platform.
			if (matching.isEmpty()) {
				promise.resolve(false)
				return
			}
			for (info in matching) {
				val ids = appWidgetManager.getAppWidgetIds(info.provider)
				if (ids.isNotEmpty()) {
					sendUpdateBroadcast(info.provider.className, ids)
				}
			}
			promise.resolve(true)
		} catch (e: Exception) {
			promise.reject("reload_widget_failed", e.message, e)
		}
	}

	fun getInstalledWidgets(promise: Promise) {
		try {
			val appWidgetManager = AppWidgetManager.getInstance(context)
			val widgets = Arguments.createArray()
			for (info in ourProviders(appWidgetManager)) {
				val kind = simpleName(info.provider.className)
				for (id in appWidgetManager.getAppWidgetIds(info.provider)) {
					widgets.pushMap(Arguments.createMap().apply {
						putString("kind", kind)
						// `family` is an iOS-only concept.
						putNull("family")
						putString("id", id.toString())
					})
				}
			}
			promise.resolve(widgets)
		} catch (e: Exception) {
			promise.reject("get_installed_widgets_failed", e.message, e)
		}
	}

	// `installedProviders` + filter rather than `getInstalledProvidersForPackage`, which is API 26+.
	private fun ourProviders(appWidgetManager: AppWidgetManager): List<AppWidgetProviderInfo> {
		return appWidgetManager.installedProviders.filter {
			it.provider.packageName == context.packageName
		}
	}

	private fun matchesKind(info: AppWidgetProviderInfo, kind: String): Boolean {
		val className = info.provider.className
		return simpleName(className).equals(kind, ignoreCase = true) ||
			className.equals(kind, ignoreCase = true)
	}

	private fun simpleName(className: String): String = className.substringAfterLast('.')

	// Explicit, same-UID intent: it reaches a receiver declared android:exported="false".
	private fun sendUpdateBroadcast(className: String, ids: IntArray) {
		val intent = Intent(context, Class.forName(className)).apply {
			action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
			putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
		}
		context.sendBroadcast(intent)
	}

	companion object {
		const val NAME = "ReloadWidget"
	}
}
