package com.reloadwidget

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.NativeModule
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.module.model.ReactModuleInfo
import java.util.HashMap

class ReloadWidgetPackage : BaseReactPackage() {
	override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
		return if (name == ReloadWidgetModuleImpl.NAME) {
			ReloadWidgetModule(reactContext)
		} else {
			null
		}
	}

	override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
		return ReactModuleInfoProvider {
			val moduleInfos: MutableMap<String, ReactModuleInfo> = HashMap()
			val isTurboModule: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
			moduleInfos[ReloadWidgetModuleImpl.NAME] = ReactModuleInfo(
				ReloadWidgetModuleImpl.NAME,
				ReloadWidgetModuleImpl.NAME,
				false,  // canOverrideExistingModule
				false,  // needsEagerInit
				false,  // isCxxModule
				isTurboModule // isTurboModule
			)
			moduleInfos
		}
	}
}
