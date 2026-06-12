import { myUserId } from 'lib';
import { isEquivalent } from 'lib/algo';
import { throttle } from 'lib/async';
import { jsonSimple } from 'lib/xhr';

export class Settings {
  constructor(
    public readonly syncSettings = false,
    public readonly showGauge = true,
    public readonly inline = false,
    public readonly showStaticAnalysis = true,
    public readonly disclosureMode = false,
    public readonly showLiveGlyphs = true,
    public readonly showBestMoveArrows = true,
    public readonly showManeuverMoveArrows = true,
    public readonly showVariationArrows = true,
    public readonly showMoveAnnotationsOnBoard = true,
    public readonly showUndefendedPieces = true,
    public readonly showPinnedPieces = true,
    public readonly showCheckableKing = true,
  ) {}
}

const defaultSettings = Object.freeze(new Settings());

export type SettingKey = keyof Settings;

export function makeSettings(fromServer?: Settings, redraw?: () => void): SettingCtrl {
  return new SettingCtrl(fromServer ?? null, redraw); // null for behavioral compatibility with play json
}

export async function fetchSettings(): Promise<SettingCtrl> {
  return makeSettings(await jsonSimple('/account/pref-json/analysisSettings').catch(() => null));
}

export class SettingCtrl extends Settings {
  private readonly key = ['analyse', myUserId(), 'settings'].filter(Boolean).join('.');
  private throttledSave = throttle(1000, () => this.save()); // key repeats

  constructor(
    private fromServer: Settings | null = null,
    public readonly redraw?: () => void,
  ) {
    super();
    const local = localStorage.getItem(this.key);
    if (!local) Object.assign(this, grandfatheredOptions()); // delete me
    if (fromServer) Object.assign(this, fromServer);
    else if (local) Object.assign(this, JSON.parse(local));
    this.set('syncSettings', fromServer !== null, 'noop');
  }

  keys(): SettingKey[] {
    return Object.keys(defaultSettings) as SettingKey[];
  }

  set<K extends SettingKey>(key: K, value: Settings[K], onChange: (() => void) | 'save' | 'noop' = 'save') {
    const oldValue = this[key];
    this[key] = value;
    if (oldValue === value || onChange === 'noop') return;
    if (onChange === 'save') {
      this.redraw?.();
      this.throttledSave();
    } else onChange();
  }

  async save() {
    const local = Object.fromEntries(this.keys().map(k => [k, this[k]])) as unknown as Settings;
    localStorage.setItem(this.key, JSON.stringify(local));

    if (this.syncSettings && isEquivalent(local, this.fromServer)) return;
    if (!this.syncSettings && !this.fromServer) return;

    const updatedServerOptions = this.syncSettings ? local : null;
    const rsp = await fetch('/account/pref-json/analysisSettings', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updatedServerOptions),
    });
    if (rsp.ok) this.fromServer = updatedServerOptions;
    else console.log(rsp.statusText, await rsp.text());
  }
}

// delete me soon
function grandfatheredOptions(): Settings {
  return {
    syncSettings: false,
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
