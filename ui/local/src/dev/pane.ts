import type { PaneInfo, EditorHost, ObjectSelector, PaneArgs } from './types';
import type { Mapping, Book } from '../types';
import { removeObjectProperty, setObjectProperty } from './util';
import { getSchemaDefault } from './schema';

export abstract class Pane {
  readonly host: EditorHost;
  readonly info: PaneInfo;
  readonly el: HTMLElement;
  readonly parent: Pane | undefined;
  enabledCheckbox?: HTMLInputElement;

  constructor({ host, info, parent }: PaneArgs) {
    [this.host, this.info, this.parent] = [host, info, parent];
    this.el = document.createElement(this.isFieldset ? 'fieldset' : 'div');
    this.el.id = this.id;
    info.class?.forEach(c => this.el.classList.add(c));
    if (info.title) this.el.title = info.title;
    host.editor.add(this);
  }

  abstract setEnabled(enabled?: boolean): void;

  abstract canEnable(): boolean;

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
    return this.parent?.info.type === 'radioGroup' ? this.parent.id : undefined;
  }

  get requires() {
    return this.info.requires ?? [];
  }

  get isFieldset() {
    return this.info.type === 'group' || this.info.type === 'books'; // || this.info.type === 'moveSelector';
  }

  get requirementsAllow(): boolean {
    return this.requires.every(r => this.host.editor.byId[r].enabled);
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

  get paneValue(): number | string | Mapping | Book[] | undefined {
    return undefined; // paneValue is from an uncommitted input control
  }

  update(_?: Event) {
    this.setProperty(this.paneValue);
    this.setEnabled(this.getProperty() !== undefined);
    this.host.update();
  }

  select() {}

  setProperty(value: string | number | Mapping | Book[] | undefined) {
    if (value === undefined) {
      if (this.paneValue) removeObjectProperty({ obj: this.host.bot, path: { id: this.id } });
    } else setObjectProperty({ obj: this.host.bot, path: { id: this.id }, value });
  }

  getProperty(sel: ObjectSelector[] = ['bot']): string | number | Mapping | Book[] | undefined {
    for (const s of sel) {
      const prop =
        s === 'schema'
          ? getSchemaDefault(this.id)
          : this.path.reduce((o, key) => o?.[key], s === 'bot' ? this.host.bot : this.host.defaultBot);
      if (prop !== undefined) return s === 'bot' ? prop : structuredClone(prop);
    }
    return undefined;
  }

  getStringProperty(sel: ObjectSelector[] = ['bot']) {
    const prop = this.getProperty(sel);
    return typeof prop === 'object' ? JSON.stringify(prop) : prop !== undefined ? String(prop) : '';
  }
}
