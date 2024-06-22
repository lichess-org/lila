import { botSchema, reservedKeys } from './botSchema';
import type { SettingHost, BaseInfo, AnyKey } from './types';
import { SettingView, SelectView, RangeView, TextareaView, TextView, NumberView } from './setting';
import { MappingNode } from './mappingNode';

export class SettingCtrl {
  byId: { [id: string]: SettingView } = {};

  byEl = (el: Element) => {
    while (el && this.byId[el.id] === undefined) el = el.parentElement!;
    return el ? this.byId[el.id] : undefined;
  };

  byEvent = (e: Event) => this.byEl(e.target as Element);

  add = (setting: SettingView) => (this.byId[setting.div.id] = setting);

  forEach(cb: (value: SettingView, key: string) => void) {
    Object.keys(this.byId).forEach(key => cb(this.byId[key], key));
  }

  *[Symbol.iterator]() {
    for (const v of Object.values(this.byId)) yield v;
  }

  requires(idOrGroup: string): SettingView[] {
    return [...this].filter(
      setting => Array.isArray(setting.info.require) && setting.info.require.includes(idOrGroup),
    );
  }

  get selectable(): SettingView[] {
    return [...this].filter(s => s.info.class?.includes('selectable'));
  }

  toggleEnabled: ActionListener = (_, __, e) => {
    const setting = this.byEvent(e)!;
    setting.setProperty(setting.inputValue);
    setting.setEnabled((e.target as HTMLInputElement).checked);
    setting.host.update();
  };

  updateProperty: ActionListener = (_, __, e) => {
    const setting = this.byEvent(e)!;
    setting.update(e);
    setting.host.update();
  };

  selectMapping: ActionListener = (_, __, e) => {
    this.selectable.forEach(s => s.div.classList.remove('selected'));
    const setting = this.byEvent(e)!;
    setting.select();
  };

  get actions(): Action[] {
    return [
      { selector: '[data-type]', event: ['input', 'change'], listener: this.updateProperty },
      { selector: '.toggle-enabled', event: 'change', listener: this.toggleEnabled },
    ];
  }
}

export function buildFromSchema(host: SettingHost, path: string[], radioGroup?: string): SettingView {
  const id = path.join('_');
  const iter = path.reduce<any>((acc, key) => acc[key], botSchema);
  const s = buildView(host, { id, radioGroup, ...iter });
  for (const key of Object.keys(iter).filter(k => !reservedKeys.includes(k as AnyKey))) {
    s.div.appendChild(
      buildFromSchema(host, [...path, key], s.info?.type === 'radioGroup' ? id : undefined).div,
    );
  }
  return s;
}

function buildView(host: SettingHost, info: BaseInfo) {
  const p = { host, info };
  switch (info?.type) {
    case 'select':
      return new SelectView(p);
    case 'range':
      return new RangeView(p);
    case 'textarea':
      return new TextareaView(p);
    case 'text':
      return new TextView(p);
    case 'number':
      return new NumberView(p);
    case 'mapping':
      return new MappingNode(p);
    default:
      return new SettingView(p);
  }
}
