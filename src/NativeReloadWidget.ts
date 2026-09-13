import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type InstalledWidget = {
	kind: string;
	family: string | null;
	id: string | null;
};

export interface Spec extends TurboModule {
	reloadAllWidgets(): Promise<boolean>;
	reloadWidget(kind: string): Promise<boolean>;
	getInstalledWidgets(): Promise<Array<InstalledWidget>>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReloadWidget');
