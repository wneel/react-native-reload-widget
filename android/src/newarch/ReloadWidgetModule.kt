package com.reloadwidget

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext

class ReloadWidgetModule (context: ReactApplicationContext) :
	NativeReloadWidgetSpec(context) {
		val moduleImpl = ReloadWidgetModuleImpl(context)

	override fun reloadAllWidgets(promise: Promise) {
		moduleImpl.reloadAllWidgets(promise)
	}

	override fun reloadWidget(kind: String, promise: Promise) {
		moduleImpl.reloadWidget(kind, promise)
	}

	override fun getInstalledWidgets(promise: Promise) {
		moduleImpl.getInstalledWidgets(promise)
	}
}
