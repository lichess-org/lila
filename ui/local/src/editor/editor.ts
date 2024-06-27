import { schema, primitiveKeys } from './schema';
import type { EditorHost, PaneInfo, AnyKey } from './types';
import type { Pane } from './pane';
import { ActionListener, Action } from 'common/dialog';
import { Setting, SelectSetting, RangeSetting, TextareaSetting, TextSetting, NumberSetting } from './setting';
import { SelectorPanel } from './panel';

export class Editor {
  byId: { [id: string]: Pane } = {};

  byEl = (el: Element) => {
    while (el && this.byId[el.id] === undefined) el = el.parentElement!;
    return el ? this.byId[el.id] : undefined;
  };

  byEvent = (e: Event) => (e.target instanceof Element ? this.byEl(e.target) : undefined);

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

  toggleEnabled: ActionListener = (_, __, e) => {
    const pane = this.byEvent(e)!;
    pane.setProperty(pane.paneValue);
    pane.setEnabled((e.target as HTMLInputElement).checked);
    (e.target as HTMLInputElement).checked = pane.enabled;
    pane.host.update();
  };

  updateProperty: ActionListener = (_, __, e) => {
    const pane = this.byEvent(e)!;
    pane.update(e);
    pane.host.update();
  };

  get actions(): Action[] {
    return [
      { selector: '[data-type]', event: ['input', 'change'], listener: this.updateProperty },
      { selector: 'canvas', event: 'click', listener: this.updateProperty },
      { selector: '.by', event: 'click', listener: this.updateProperty },
      { selector: '.toggle-enabled', event: 'change', listener: this.toggleEnabled },
    ];
  }
}

export function buildFromSchema(host: EditorHost, path: string[], parent?: Pane): Pane {
  const id = path.join('_');
  const iter = path.reduce<any>((acc, key) => acc[key], schema);
  const s = buildFromInfo(host, { id, ...iter }, parent);
  for (const key of Object.keys(iter).filter(k => !primitiveKeys.includes(k as AnyKey))) {
    s.div.appendChild(buildFromSchema(host, [...path, key], s).div);
  }
  return s;
}

function buildFromInfo(host: EditorHost, info: PaneInfo, parent?: Pane): Pane {
  const p = { host, info, parent };
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
    case 'selectorPanel':
      return new SelectorPanel(p);
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
