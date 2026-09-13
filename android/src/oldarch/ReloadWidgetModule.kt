package com.reloadwidget

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactMethod

class ReloadWidgetModule internal constructor(context: ReactApplicationContext) :
	ReactContextBaseJavaModule(context) {
	val moduleImpl = ReloadWidgetModuleImpl(context)

	@ReactMethod(isBlockingSynchronousMethod = false)
	fun reloadAllWidgets(promise: Promise) {
		moduleImpl.reloadAllWidgets(promise)
	}

	@ReactMethod(isBlockingSynchronousMethod = false)
	fun reloadWidget(kind: String, promise: Promise) {
		moduleImpl.reloadWidget(kind, promise)
	}

	@ReactMethod(isBlockingSynchronousMethod = false)
	fun getInstalledWidgets(promise: Promise) {
		moduleImpl.getInstalledWidgets(promise)
	}

	override fun getName(): String {
		return ReloadWidgetModuleImpl.NAME
	}
}
