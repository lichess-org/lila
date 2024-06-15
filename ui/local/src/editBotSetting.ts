import { EditBotDialog } from './editBotDialog';
import { BaseInfo, SelectInfo, TextareaInfo, NumberInfo, RangeInfo, getSchemaDefault } from './editBotSchema';

type BaseParams = { info: BaseInfo; dlg: EditBotDialog };

export function buildSetting(info: BaseInfo, dlg: EditBotDialog) {
  const p = { info, dlg };
  switch (info?.type) {
    case 'select':
      return new SelectSetting(p);
    case 'range':
      return new RangeSetting(p);
    case 'textarea':
      return new TextareaSetting(p);
    case 'text':
      return new TextSetting(p);
    case 'number':
      return new NumberSetting(p);
    default:
      return new SettingNode(p);
  }
}

export class Settings {
  byId: { [id: string]: SettingNode } = {};
  byEl = (el: Element) => {
    while (el && this.byId[el.id] === undefined) el = el.parentElement!;
    return el ? this.byId[el.id] : undefined;
  };
  byEvent = (e: Event) => this.byEl(e.target as Element);
  add = (setting: SettingNode) => (this.byId[setting.div.id] = setting);
  *[Symbol.iterator]() {
    for (const v of Object.values(this.byId)) yield v;
  }
  forEach(cb: (value: SettingNode, key: string) => void) {
    Object.keys(this.byId).forEach(key => cb(this.byId[key], key));
  }
}

export class SettingNode {
  readonly info: BaseInfo;
  readonly dlg: EditBotDialog;
  readonly div: HTMLElement;
  toggle?: HTMLInputElement;
  input?: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

  constructor({ info, dlg }: BaseParams) {
    [this.info, this.dlg] = [info, dlg];
    this.div = document.createElement(info.type || !info.label ? 'div' : 'fieldset');
    this.div.id = this.id;
    dlg.settings.add(this);
    info.class?.forEach(c => this.div.classList.add(c));
    if (info.type) this.div.classList.add('setting');
    if (info.title) this.div.title = info.title;
    if (info.radioGroup) this.div.dataset.radioGroup = info.radioGroup;
    if (info.label) {
      const label = document.createElement(info.type ? 'label' : 'legend');
      label.textContent = info.label;
      this.div.appendChild(label);
    }
  }

  init() {
    if (!this.input) return;
    const toggleAttrs = this.info.radioGroup
      ? `type="radio" name="${this.info.radioGroup}" tabindex="-1"`
      : this.info.require !== true
      ? 'type="checkbox"'
      : undefined;
    if (toggleAttrs) {
      this.toggle = $as<HTMLInputElement>(`<input ${toggleAttrs} class="toggle-enabled">`);
      this.toggle.checked = this.getProperty() !== undefined;
      this.div.prepend(this.toggle);
      //if (this.info.radioGroup) (this.dlg.radios[this.info.radioGroup] ??= []).push(this.id);
    }

    this.input.value = String(this.getProperty() ?? this.getProperty(this.dlg.botDefault) ?? '');
    this.setEnabled();
    this.div.appendChild(this.input);
  }

  setEnabled(enabled?: boolean) {
    const { settings, view } = this.dlg;
    if (!this.input) return;
    if (enabled === !this.div.classList.contains('disabled')) return;
    if (enabled === undefined) enabled = this.enabled;
    this.div.classList.toggle('disabled', !enabled);
    this.setProperty(enabled ? this.input.value : undefined);
    // if (!enabled) {
    //   const latentEnabled = requires.find(dep => this.dlg.els[dep].enabled);
    //   if (latentEnabled) {
    //     this.dlg.els[latentEnabled].setEnabled(false);
    //     requires.splice(requires.indexOf(latentEnabled), 1);
    //     requires.push(latentEnabled);
    //   }
    // }
    //this.dlg.requires[this.id]?.forEach(id => this.dlg.els[id].setVisibility());
    settings.forEach(el => el.setVisibility());
    if (this.toggle) this.toggle.checked = enabled;
    if (this.info.radioGroup && enabled)
      view.querySelectorAll(`[data-radio-group="${this.info.radioGroup}"]`).forEach(el => {
        const setting = settings.byEl(el);
        if (setting === this) return;
        setting?.setEnabled(false);
      });
  }

  setVisibility() {
    this.div.classList.toggle(
      'none',
      Array.isArray(this.info.require) && this.info.require.some(id => !this.dlg.settings.byId[id].enabled),
    );
  }

  update() {
    this.setProperty(this.input?.value);
  }

  get inputValue(): number | string | undefined {
    return this.input?.value;
  }

  get id() {
    return this.info.id!;
  }

  /*get require() {
    return typeof this.info.require === 'boolean'
      ? this.info.require
      : this.info.require?.every(id => this.dlg.els[id].getProperty()) || true;
  }*/

  get enabled() {
    return this.getProperty() !== undefined;
  }

  setProperty(value: string | number | undefined) {
    const path = objectPath(this.id);
    if (value === undefined) removePath({ obj: this.dlg.bot, path });
    else setPath({ obj: this.dlg.bot, path, value });
  }

  getProperty(of = this.dlg.bot as any) {
    return objectPath(this.id)?.reduce((obj, key) => obj?.[key], of);
  }
}

class SelectSetting extends SettingNode {
  input = $as<HTMLSelectElement>('<select data-type="string">');
  info: SelectInfo;
  constructor(p: BaseParams) {
    super(p);
    this.init();
    for (const c of this.info.choices) {
      const option = document.createElement('option');
      option.value = c.value; //JSON.stringify(c.value);
      option.textContent = c.name;
      if (option.value === this.getProperty()) option.selected = true;
      this.input.appendChild(option);
    }
  }
  get inputValue(): string {
    return this.input.value;
  }
  getProperty(of = this.dlg.bot as any) {
    return JSON.stringify(super.getProperty(of));
  }
  setProperty(value: string | undefined) {
    super.setProperty(value ? JSON.parse(value) : undefined);
  }
}

class TextSetting extends SettingNode {
  input = $as<HTMLInputElement>('<input type="text" data-type="string">');
  constructor(p: BaseParams) {
    super(p);
    this.init();
  }
  get inputValue(): string {
    return this.input.value;
  }
}

class TextareaSetting extends SettingNode {
  input = $as<HTMLTextAreaElement>('<textarea data-type="string">');
  info: TextareaInfo;
  constructor(p: BaseParams) {
    super(p);
    this.init();
    if (this.info.rows) this.input.rows = this.info.rows;
  }
  get inputValue(): string {
    return this.input.value;
  }
}

class NumberSetting extends SettingNode {
  input = $as<HTMLInputElement>('<input type="text" data-type="number">');
  info: NumberInfo | RangeInfo;
  constructor(p: BaseParams) {
    super(p);
    this.init();
    this.input.min = String(this.info.min);
    this.input.max = String(this.info.max);
  }
  update() {
    const prop = Number(this.input.value);
    if (isNaN(prop) || prop < this.info.min || prop > this.info.max) {
      this.input.classList.add('invalid');
      this.setProperty(undefined);
      return;
    }
    this.input.classList.remove('invalid');
    this.setProperty(prop);
  }
  get inputValue(): number | undefined {
    const v = Number(this.input.value);
    return isNaN(v) ? undefined : v;
  }
}

class RangeSetting extends NumberSetting {
  preview: HTMLElement;
  info: RangeInfo;
  constructor(p: BaseParams) {
    super(p);
    this.input.type = 'range';
    this.input.step = String(this.info.step);
    this.preview = $as<HTMLElement>(`<label>${this.getProperty() ?? ''}</label`);
    this.div.insertBefore(this.preview, this.input);
  }
  update() {
    super.update();
    this.preview.textContent = this.input.value;
  }
}

function objectPath(id: string) {
  return id.split('_').slice(1);
}

function removePath({ obj, path }: { obj: any; path: string[] }) {
  if (!obj) return;
  if (path.length > 1) removePath({ obj: obj[path[0]], path: path.slice(1) });
  if (typeof obj[path[0]] !== 'object' || Object.keys(obj[path[0]]).length === 0) delete obj[path[0]];
}

function setPath({ obj, path, value }: { obj: any; path: string[]; value: any }) {
  if (path.length === 0) return;
  if (path.length === 1) obj[path[0]] = value;
  else if (!(path[0] in obj)) obj[path[0]] = {};
  setPath({ obj: obj[path[0]], path: path.slice(1), value });
}
