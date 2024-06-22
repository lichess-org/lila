import { NodeArgs, SettingHost, BaseInfo, SelectInfo, TextareaInfo, NumberInfo, RangeInfo } from './types';
import { Mapping } from '../types';
import { getSchemaDefault } from './botSchema';

export class SettingView {
  readonly host: SettingHost;
  readonly info: BaseInfo;
  readonly div: HTMLElement;
  enabledCheckbox?: HTMLInputElement;
  input?: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

  constructor({ host, info }: NodeArgs) {
    [this.host, this.info] = [host, info];
    this.div = document.createElement(info.type || !info.label ? 'div' : 'fieldset');
    this.div.id = this.id;
    host.settings.add(this);
    info.class?.forEach(c => this.div.classList.add(c));
    if (this.isSetting) this.div.classList.add('setting');
    if (info.title) this.div.title = info.title;
    let label;
    if (info.label) {
      label = document.createElement(this.isSetting ? 'label' : 'legend');
      label.textContent = info.label;
      this.div.appendChild(label);
    }
    if (this.info.radioGroup)
      this.enabledCheckbox = $as<HTMLInputElement>(
        `<input type="radio" name="${this.info.radioGroup}" tabindex="-1">`,
      );
    else if (this.info.require !== true && label)
      this.enabledCheckbox = $as<HTMLInputElement>(`<input type="checkbox">`);
    if (this.enabledCheckbox) {
      this.enabledCheckbox.classList.add('toggle-enabled');
      this.enabledCheckbox.checked = this.getProperty() !== undefined; // ?
      if (this.div.tagName === 'FIELDSET') label?.prepend(this.enabledCheckbox);
      else this.div.prepend(this.enabledCheckbox as HTMLInputElement);
    }
  }

  init() {
    this.setEnabled();
    if (this.input) this.div.appendChild(this.input);
  }

  setEnabled(enabled = this.canEnable()) {
    if (!this.input && !this.enabledCheckbox) return;
    const { settings, view } = this.host;
    this.div.classList.toggle('disabled', !enabled);
    if (this.input && !this.input.value && enabled !== 'refresh')
      this.input.value = this.getStringProperty(['bot', 'default', 'schema']);
    if (enabled) {
      this.host.bot.disabled.delete(this.id);
      for (const setting of this.requires.map(x => settings.byId[x])) {
        if (setting?.info.require === true) setting.update();
        else if (setting?.info.type !== 'radioGroup') continue;
        const radios = Object.values(settings.byId).filter(x => x.info.radioGroup === setting.id);
        const active = radios?.find(x => x.enabled) ?? radios?.find(x => x.getProperty(['default']));
        if (active) active.update();
        else if (radios.length) radios[0].update();
      }
    } else if (this.enabledCheckbox) this.host.bot.disabled.add(this.id);
    if (this.enabledCheckbox) this.enabledCheckbox.checked = !!enabled;
    if (this.info.radioGroup && enabled)
      view.querySelectorAll(`[name="${this.info.radioGroup}"]`).forEach(el => {
        const setting = settings.byEl(el);
        if (setting === this) return;
        setting?.setEnabled(false);
      });
    for (const id in settings.byId) {
      if (id.startsWith(this.id) && id.split('_').length === this.id.split('_').length + 1) {
        settings.byId[id].div.classList.toggle('none', !enabled);
      }
    }
  }

  update(_?: Event) {
    this.setProperty(this.inputValue);
    this.setEnabled(this.getProperty() !== undefined);
  }

  select() {}

  get inputValue(): number | string | Mapping | undefined {
    return this.input?.value;
  }

  get isSetting() {
    return this.info.type && this.info.type !== 'radioGroup';
  }

  get id() {
    return this.info.id!;
  }

  get requires() {
    return Array.isArray(this.info.require) ? this.info.require : [];
  }

  get enabled() {
    return (
      !this.host.bot.disabled.has(this.id) &&
      (this.info.require === true || this.getProperty() !== undefined || !this.isSetting)
    );
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

  private canEnable(): boolean | 'refresh' {
    if (!this.input && Array.isArray(this.info.require)) {
      for (const r of this.info.require) {
        const setting = this.host.settings.byId[r];
        if (setting && setting.getProperty() === undefined) return false;
        else if (!setting) {
          const radios = Object.values(this.host.settings.byId).filter(x => x.info.radioGroup === r);
          if (!radios.find(x => x.getProperty())) return false;
        }
      }
    } else return this.enabled;
    return 'refresh';
  }
}

export class DisclosureView extends SettingView {
  constructor(p: NodeArgs) {
    super(p);
    this.init();
  }
}

export class SelectView extends SettingView {
  input = $as<HTMLSelectElement>('<select data-type="string">');
  info: SelectInfo;
  constructor(p: NodeArgs) {
    super(p);
    for (const c of this.info.choices) {
      const option = document.createElement('option');
      option.value = c.value;
      option.textContent = c.name;
      if (option.value === this.getStringProperty()) option.selected = true;
      this.input.appendChild(option);
    }
    this.div.appendChild(document.createElement('hr'));
    this.init();
  }
  get inputValue(): string {
    return this.input.value;
  }
}

export class TextView extends SettingView {
  input = $as<HTMLInputElement>('<input type="text" data-type="string">');
  constructor(p: NodeArgs) {
    super(p);
    this.init();
  }
  get inputValue(): string {
    return this.input.value;
  }
}

export class TextareaView extends SettingView {
  input = $as<HTMLTextAreaElement>('<textarea data-type="string">');
  info: TextareaInfo;
  constructor(p: NodeArgs) {
    super(p);
    this.init();
    if (this.info.rows) this.input.rows = this.info.rows;
  }
  get inputValue(): string {
    return this.input.value;
  }
}

export class NumberView extends SettingView {
  input = $as<HTMLInputElement>('<input type="text" data-type="number">');
  info: NumberInfo | RangeInfo;
  constructor(p: NodeArgs) {
    super(p);
    this.div.appendChild(document.createElement('hr'));
    this.init();
    this.input.maxLength = maxChars(this.info);
    this.input.style.maxWidth = `calc(${maxChars(this.info)}ch + 1.5em)`;
    if (this.info.min === this.info.max) this.input.disabled = true;
  }
  update() {
    const isValid = this.isValid();
    this.input.classList.toggle('invalid', !isValid);
    if (isValid) {
      this.setProperty(this.inputValue);
      this.setEnabled(true);
    }
  }
  isValid(el: HTMLInputElement = this.input) {
    const v = Number(el.value);
    return !isNaN(v) && v >= this.info.min && v <= this.info.max;
  }
  get inputValue(): number | undefined {
    return this.isValid() ? Number(this.input.value) : undefined;
  }
}

export class RangeView extends NumberView {
  rangeInput = $as<HTMLInputElement>('<input type="range" data-type="number">');
  info: RangeInfo;
  constructor(p: NodeArgs) {
    super(p);
    this.rangeInput.min = String(this.info.min);
    this.rangeInput.max = String(this.info.max);
    this.rangeInput.step = String(this.info.step);
    this.rangeInput.value = this.input.value;
    this.div.querySelector('hr')?.replaceWith(this.rangeInput);
  }
  update(e?: Event) {
    if (!e || e.target === this.input) {
      super.update();
      this.rangeInput.value = this.input.value;
      return;
    }
    this.input.value = this.rangeInput.value;
    this.input.classList.remove('invalid');
    this.setProperty(Number(this.input.value));
    this.setEnabled(true);
  }
}

export type ObjectSelector = 'bot' | 'default' | 'schema';

export function idToPath({ id, obj }: { id: string; obj: any }): string[] {
  const path = id.split('_');
  return path[0] in obj ? path : path.slice(1);
}

export function resolvePath({ obj, path }: { obj: any; path: string[] }) {
  return path.reduce((o, key) => o?.[key], obj);
}

export function removePath({ obj, path }: { obj: any; path: string[] }, stripObjects = false) {
  if (!obj) return;
  if (path.length > 1) removePath({ obj: obj[path[0]], path: path.slice(1) });
  if (typeof obj[path[0]] !== 'object' || (stripObjects && path.length === 1)) delete obj[path[0]];
}

export function setPath({ obj, path, value }: { obj: any; path: string[]; value: any }) {
  if (path.length === 0) return;
  if (path.length === 1) obj[path[0]] = value;
  else if (!(path[0] in obj)) obj[path[0]] = {};
  setPath({ obj: obj[path[0]], path: path.slice(1), value });
}

export function maxChars(info: NumberInfo | RangeInfo) {
  const len = Math.max(info.max.toString().length, info.min.toString().length);
  if (!('step' in info)) return len;
  const fractionLen = info.step < 1 ? String(info.step).length - String(info.step).indexOf('.') - 1 : 0;
  return len + fractionLen;
}
