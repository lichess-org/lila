import type { BaseInfo, EditorHost, ObjectSelector, PaneArgs } from './types';
import type { Mapping } from '../types';
import { removePath, setPath } from './util';
import { getSchemaDefault } from './schema';

export abstract class Pane {
  readonly host: EditorHost;
  readonly info: BaseInfo;
  readonly div: HTMLElement;
  enabledCheckbox?: HTMLInputElement;

  get isSetting() {
    return this.info.type?.endsWith('Setting') ?? false;
  }

  get id() {
    return this.info.id!;
  }

  get requires() {
    return this.info.requires ?? [];
  }

  constructor({ host, info }: PaneArgs) {
    [this.host, this.info] = [host, info];
    this.div = document.createElement(info.type || !info.label ? 'div' : 'fieldset');
    this.div.id = this.id;
    host.editor.add(this);
    info.class?.forEach(c => this.div.classList.add(c));
    if (this.isSetting) this.div.classList.add('setting');
    if (info.title) this.div.title = info.title;
  }

  update(_?: Event) {
    this.setProperty(this.inputValue);
    this.setEnabled(this.getProperty() !== undefined);
  }

  select() {}

  abstract get inputValue(): number | string | Mapping | undefined;

  get children() {
    return Object.keys(this.host.editor.byId)
      .filter(id => id.startsWith(this.id) && id.split('_').length === this.id.split('_').length + 1)
      .map(id => this.host.editor.byId[id]);
  }

  abstract setEnabled(enabled?: boolean): void;

  get enabled(): boolean {
    if (this.host.bot.disabled.has(this.id)) return false;
    const kids = this.children;
    if (!kids.length) return this.getProperty() !== undefined;
    return kids.every(x => x.enabled || !x.info.required);
  }

  get path() {
    return this.id.split('_').slice(1);
  }

  setProperty(value: string | number | Mapping | undefined) {
    if (value === undefined) removePath({ obj: this.host.bot, path: this.path });
    else setPath({ obj: this.host.bot, path: this.path, value });
  }

  getProperty(sel: ObjectSelector[] = ['bot']) {
    for (const s of sel) {
      const prop =
        s === 'schema'
          ? getSchemaDefault(this.id)
          : this.path.reduce((o, key) => o?.[key], s === 'bot' ? this.host.bot : this.host.botDefault);
      if (prop !== undefined) return prop;
    }
    return undefined;
  }

  getStringProperty(sel: ObjectSelector[] = ['bot']) {
    const prop = this.getProperty(sel);
    return typeof prop === 'object' ? JSON.stringify(prop) : prop !== undefined ? String(prop) : '';
  }

  //protected abstract autoEnable(): boolean;
}
