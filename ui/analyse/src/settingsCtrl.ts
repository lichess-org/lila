import { myUserId } from 'lib';

export class Settings {
  constructor(
    public readonly showGauge = true,
    public readonly inline = false,
    public readonly showStaticAnalysis = true,
    public readonly disclosureMode = false,
    public readonly showLiveAnnotations = false,
    public readonly showBestMoveArrows = true,
    public readonly showManeuverMoveArrows = false,
    public readonly showVariationArrows = true,
    public readonly showMoveAnnotationsOnBoard = true,
    public readonly showUndefendedPieces = false,
    public readonly showPinnedPieces = false,
    public readonly showCheckableKing = false,
    public readonly useCloudEval = true,
  ) {}
}

const defaultSettings = Object.freeze(new Settings());

export type SettingKey = keyof Settings;

export class SettingsCtrl extends Settings {
  private readonly storageKey = ['analyse', myUserId(), 'settings'].filter(Boolean).join('.');

  constructor(public readonly onUpdateSettings?: (key?: SettingKey) => void) {
    super();
    const local = localStorage.getItem(this.storageKey);
    try {
      if (local) Object.assign(this, JSON.parse(local));
    } catch {
      localStorage.removeItem(this.storageKey);
    }
  }

  keys(): SettingKey[] {
    return Object.keys(defaultSettings) as SettingKey[];
  }

  set<K extends SettingKey>(key: K, value: Settings[K]) {
    const oldValue = this[key];
    if (oldValue === value) return;
    this[key] = value;
    this.onUpdateSettings?.(key);
    this.save();
  }

  async save() {
    // POST to server once db.pref is decided
    const local = Object.fromEntries(this.keys().map(k => [k, this[k]])) as unknown as Settings;
    localStorage.setItem(this.storageKey, JSON.stringify(local));
  }
}
