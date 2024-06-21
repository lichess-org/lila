import { botSchema, reservedKeys } from './botSchema';
import type { SettingHost, BaseInfo } from './types';
import {
  SettingNode,
  SelectNode,
  RangeNode,
  TextareaNode,
  TextNode,
  NumberNode,
  DisclosureNode,
} from './settingNode';
import { MappingNode } from './mappingNode';

export class SettingGroup {
  byId: { [id: string]: SettingNode } = {};

  byEl = (el: Element) => {
    while (el && this.byId[el.id] === undefined) el = el.parentElement!;
    return el ? this.byId[el.id] : undefined;
  };

  byEvent = (e: Event) => this.byEl(e.target as Element);

  add = (setting: SettingNode) => (this.byId[setting.div.id] = setting);

  forEach(cb: (value: SettingNode, key: string) => void) {
    Object.keys(this.byId).forEach(key => cb(this.byId[key], key));
  }

  *[Symbol.iterator]() {
    for (const v of Object.values(this.byId)) yield v;
  }

  requires(idOrGroup: string): SettingNode[] {
    return [...this].filter(
      setting => Array.isArray(setting.info.require) && setting.info.require.includes(idOrGroup),
    );
  }

  get selectable(): SettingNode[] {
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

export function buildFromSchema(host: SettingHost, path: string[] = []): SettingNode {
  const iter = path.reduce<any>((acc, key) => acc[key], botSchema);
  const s = buildNode(host, { id: path.join('_'), ...iter });
  if (iter?.type) return s;
  for (const key of Object.keys(iter).filter(k => !reservedKeys.includes(k as keyof BaseInfo))) {
    s.div.appendChild(buildFromSchema(host, [...path, key]).div);
  }
  return s;
}

function buildNode(host: SettingHost, info: BaseInfo) {
  const p = { host, info };
  switch (info?.type) {
    case 'select':
      return new SelectNode(p);
    case 'range':
      return new RangeNode(p);
    case 'textarea':
      return new TextareaNode(p);
    case 'text':
      return new TextNode(p);
    case 'number':
      return new NumberNode(p);
    case 'mapping':
      return new MappingNode(p);
    case 'disclosure':
      return new DisclosureNode(p);
    default:
      return new SettingNode(p);
  }
}
