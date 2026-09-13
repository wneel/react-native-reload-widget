import Foundation

#if canImport(WidgetKit)
import WidgetKit
#endif

/// The Swift half of react-native-reload-widget.
///
/// This class exists because WidgetKit is unreachable from Objective-C: the
/// framework's umbrella header exposes nothing, and `WidgetCenter` is declared
/// only in the Swift interface. So the TurboModule shim in ReloadWidget.mm
/// stays Objective-C++ (codegen requires that) and calls into here.
@objc(ReloadWidgetImpl)
public class ReloadWidgetImpl: NSObject {

	/// Asks WidgetKit to rebuild every widget belonging to this app.
	///
	/// Returns whether the request was dispatched. WidgetKit gives no feedback
	/// about what it did with it, so a true here means "handed to the system",
	/// not "the widget has redrawn".
	@objc public func reloadAllWidgets() -> Bool {
		#if canImport(WidgetKit)
		if #available(iOS 14.0, *) {
			WidgetCenter.shared.reloadAllTimelines()
			return (true)
		}
		#endif
		return (false)
	}

	/// Asks WidgetKit to rebuild a single kind of widget.
	///
	/// `reloadTimelines(ofKind:)` returns nothing and is a silent no-op for a
	/// kind that is not installed, so an unknown kind is indistinguishable
	/// from a known one here. This reports only whether the request could be
	/// dispatched at all. Callers who need to know what is actually placed
	/// should ask getInstalledWidgets.
	@objc public func reloadWidget(_ kind: String) -> Bool {
		#if canImport(WidgetKit)
		if #available(iOS 14.0, *) {
			WidgetCenter.shared.reloadTimelines(ofKind: kind)
			return (true)
		}
		#endif
		return (false)
	}

	/// Lists the widgets currently placed on the home screen, one entry per
	/// placed instance.
	///
	/// Resolves to an empty array rather than surfacing an error: WidgetKit
	/// fails this call on configurations that simply have no widget support,
	/// which is the same answer as "none placed" for every practical purpose.
	@objc public func getInstalledWidgets(_ completion: @escaping ([[String: Any]]) -> Void) {
		#if canImport(WidgetKit)
		if #available(iOS 14.0, *) {
			WidgetCenter.shared.getCurrentConfigurations { result in
				switch result {
					case .success(let widgets):
						completion(widgets.map { widget in
							return ([
								"kind": widget.kind,
								"family": widget.family.description,
								// Android-only: iOS has no per-instance widget id.
								"id": NSNull(),
							])
						})
					case .failure:
						completion([])
				}
			}
			return
		}
		#endif
		completion([])
	}
}
