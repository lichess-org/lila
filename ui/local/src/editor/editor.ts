import { schema, primitiveKeys } from './schema';
import type { EditorHost, BaseInfo, AnyKey } from './types';
import type { Pane } from './pane';
import { Setting, SelectSetting, RangeSetting, TextareaSetting, TextSetting, NumberSetting } from './setting';
import { MappingPanel } from './panel';

export class Editor {
  byId: { [id: string]: Pane } = {};

  byEl = (el: Element) => {
    while (el && this.byId[el.id] === undefined) el = el.parentElement!;
    return el ? this.byId[el.id] : undefined;
  };

  byEvent = (e: Event) => this.byEl(e.target as Element);

  add = (setting: Pane) => (this.byId[setting.div.id] = setting);

  forEach(cb: (value: Pane, key: string) => void) {
    Object.keys(this.byId).forEach(key => cb(this.byId[key], key));
  }

  *[Symbol.iterator]() {
    for (const v of Object.values(this.byId)) yield v;
  }

  requires(idOrGroup: string): Pane[] {
    return [...this].filter(setting => setting.info.requires?.includes(idOrGroup));
  }

  get selectable(): Pane[] {
    return [...this].filter(s => s.info.class?.includes('selectable'));
  }

  toggleEnabled: ActionListener = (_, __, e) => {
    const setting = this.byEvent(e) as Setting;
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

export function buildFromSchema(host: EditorHost, path: string[], radio?: string): Pane {
  const id = path.join('_');
  const iter = path.reduce<any>((acc, key) => acc[key], schema);
  const s = buildFromInfo(host, { id, radio, ...iter });
  for (const key of Object.keys(iter).filter(k => !primitiveKeys.includes(k as AnyKey))) {
    s.div.appendChild(buildFromSchema(host, [...path, key], s.info?.type === 'radio' ? id : undefined).div);
  }
  return s;
}

function buildFromInfo(host: EditorHost, info: BaseInfo) {
  const p = { host, info };
  switch (info?.type) {
    case 'selectSetting':
      return new SelectSetting(p);
    case 'rangeSetting':
      return new RangeSetting(p);
    case 'textareaSetting':
      return new TextareaSetting(p);
    case 'textSetting':
      return new TextSetting(p);
    case 'numberSetting':
      return new NumberSetting(p);
    case 'mappingPanel':
      return new MappingPanel(p);
    default:
      return new Setting(p);
  }
}
/*
function require(requireId: string) {
  const [id, conf] = requireId.split(/=|\^=|$=|<|>|<=|>=/).map(x => x.trim());
  const op = conf ? requireId.match(/=|\^=|$=|<|>|<=|>=/)![0] : undefined;
  // stretch goal - support advanced requirements
}
  */
