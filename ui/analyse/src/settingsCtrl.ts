import { myUserId } from 'lib';
import { throttle } from 'lib/async';

export class Settings {
  constructor(
    public readonly showGauge = true,
    public readonly inline = false,
    public readonly showStaticAnalysis = true,
    public readonly disclosureMode = false,
    public readonly showLiveGlyphs = false,
    public readonly showBestMoveArrows = true,
    public readonly showManeuverMoveArrows = false,
    public readonly showVariationArrows = true,
    public readonly showMoveAnnotationsOnBoard = true,
    public readonly showUndefendedPieces = false,
    public readonly showPinnedPieces = false,
    public readonly showCheckableKing = false,
  ) {}
}

const defaultSettings = Object.freeze(new Settings());

export type SettingKey = keyof Settings;

export class SettingsCtrl extends Settings {
  private readonly key = ['analyse', myUserId(), 'settings'].filter(Boolean).join('.');
  private readonly throttledSave = throttle(1000, () => this.save());

  constructor(public readonly redraw?: () => void) {
    super();
    const local = localStorage.getItem(this.key);
    if (local) Object.assign(this, JSON.parse(local));
    else Object.assign(this, grandfatheredOptions()); // delete me
  }

  keys(): SettingKey[] {
    return Object.keys(defaultSettings) as SettingKey[];
  }

  set<K extends SettingKey>(key: K, value: Settings[K]) {
    const oldValue = this[key];
    if (oldValue === value) return;
    this[key] = value;
    this.redraw?.();
    this.throttledSave();
  }

  async save() {
    const local = Object.fromEntries(this.keys().map(k => [k, this[k]])) as unknown as Settings;
    localStorage.setItem(this.key, JSON.stringify(local));
  }
}

// delete me soon
function grandfatheredOptions(): Settings {
  return {
    inline: legacyStorageBoolean('inline', 'treeView'),
    showBestMoveArrows: legacyStorageBoolean('showBestMoveArrows', 'analyse.auto-shapes'),
    showManeuverMoveArrows: legacyStorageBoolean('showManeuverMoveArrows', 'analyse.maneuver-arrows'),
    showLiveGlyphs: true,
    showVariationArrows: legacyStorageBoolean('showVariationArrows', 'analyse.variation-arrow-opacity'),
    showGauge: legacyStorageBoolean('showGauge', 'analyse.show-gauge'),
    showStaticAnalysis: legacyStorageBoolean('showStaticAnalysis', 'analyse.show-computer'),
    showMoveAnnotationsOnBoard: legacyStorageBoolean(
      'showMoveAnnotationsOnBoard',
      'analyse.show-move-annotation',
    ),
    showUndefendedPieces: legacyStorageBoolean('showUndefendedPieces', 'analyse.motif.undefended'),
    showPinnedPieces: legacyStorageBoolean('showPinnedPieces', 'analyse.motif.pin'),
    showCheckableKing: legacyStorageBoolean('showCheckableKing', 'analyse.motif.checkable'),
    disclosureMode: legacyStorageBoolean('disclosureMode', 'analyse.disclosure.enabled'),
  };
  function legacyStorageBoolean(optionKey: keyof Settings, storageKey: string) {
    const item = localStorage.getItem(storageKey);
    if (item === null) return defaultSettings[optionKey];
    localStorage.removeItem(storageKey);
    if (storageKey === 'analyse.variation-arrow-opacity') return Number(item) > 0;
    if (storageKey === 'treeView') return item === 'inline';
    return item === 'true';
  }
}
