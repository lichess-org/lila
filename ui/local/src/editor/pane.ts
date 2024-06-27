import type { PaneInfo, EditorHost, ObjectSelector, PaneArgs } from './types';
import type { Mapping } from '../types';
import { removePath, setPath } from './util';
import { getSchemaDefault } from './schema';

export abstract class Pane {
  readonly host: EditorHost;
  readonly info: PaneInfo;
  readonly div: HTMLElement;
  readonly parent: Pane | undefined;
  enabledCheckbox?: HTMLInputElement;

  constructor({ host, info, parent }: PaneArgs) {
    [this.host, this.info, this.parent] = [host, info, parent];
    this.div = document.createElement(this.info.type || !info.label ? 'div' : 'fieldset');
    this.div.id = this.id;
    host.editor.add(this);
    info.class?.forEach(c => this.div.classList.add(c));
    if (this.isSetting) this.div.classList.add('setting');
    if (info.title) this.div.title = info.title;
  }

  get id() {
    return this.info.id!;
  }

  get name() {
    return this.id.split('_').pop()!;
  }

  get path() {
    return this.id.split('_').slice(1);
  }

  get radioGroup() {
    return this.parent?.info.type === 'radio' ? this.parent.id : undefined;
  }

  get requires() {
    return this.info.requires ?? [];
  }

  get requirementsAllow(): boolean {
    return this.requires.every(r => this.host.editor.byId[r].enabled);
  }

  get isSetting() {
    return this.info.type?.endsWith('Setting') ?? false;
  }

  get children() {
    return Object.keys(this.host.editor.byId)
      .filter(id => id.startsWith(this.id) && id.split('_').length === this.id.split('_').length + 1)
      .map(id => this.host.editor.byId[id]);
  }

  get enabled(): boolean {
    if (this.host.bot.disabled.has(this.id)) return false;
    const kids = this.children;
    if (!kids.length) return this.getProperty() !== undefined;
    return kids.every(x => x.enabled || !x.info.required);
  }

  get paneValue(): number | string | Mapping | undefined {
    return undefined;
  }

  setEnabled(enabled?: boolean) {}

  update(_?: Event) {
    this.setProperty(this.paneValue);
    this.setEnabled(this.getProperty() !== undefined);
    this.host.update();
  }

  select() {}

  setProperty(value: string | number | Mapping | undefined) {
    if (value === undefined) {
      if (this.paneValue) removePath({ obj: this.host.bot, path: this.path });
    } else setPath({ obj: this.host.bot, path: this.path, value });
  }

  getProperty(sel: ObjectSelector[] = ['bot']): string | number | Mapping | undefined {
    for (const s of sel) {
      const prop =
        s === 'schema'
          ? getSchemaDefault(this.id)
          : this.path.reduce((o, key) => o?.[key], s === 'bot' ? this.host.bot : this.host.botDefault);
      if (prop !== undefined) return s === 'bot' ? prop : structuredClone(prop);
    }
    return undefined;
  }

  getStringProperty(sel: ObjectSelector[] = ['bot']) {
    const prop = this.getProperty(sel);
    return typeof prop === 'object' ? JSON.stringify(prop) : prop !== undefined ? String(prop) : '';
  }

  abstract autoEnable(): boolean;
}
