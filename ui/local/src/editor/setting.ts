import { botSchema, getSchemaDefault } from './botSchema';
import { Mapping } from '../types';
import { renderMapping } from './mappingView';
import {
  type SettingHost,
  type BaseInfo,
  type SelectInfo,
  type TextareaInfo,
  type NumberInfo,
  type RangeInfo,
  type MappingInfo,
  reservedKeys,
} from './types';

export function buildFromSchema(host: SettingHost, path: string[] = []): SettingNode {
  const iter = path.reduce<any>((acc, key) => acc[key], botSchema);
  const s = buildSetting(host, { id: path.join('_'), ...iter });
  if (iter?.type) return s;
  for (const key of Object.keys(iter).filter(k => !reservedKeys.includes(k as keyof BaseInfo))) {
    s.div.appendChild(buildFromSchema(host, [...path, key]).div);
  }
  return s;
}

export class SettingGroup {
  constructor() {
    console.log(botSchema.bot);
  }
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
}

export class SettingNode {
  readonly host: SettingHost;
  readonly info: BaseInfo;
  readonly div: HTMLElement;
  toggle?: HTMLInputElement;
  input?: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

  constructor({ host, info }: NodeArgs) {
    [this.host, this.info] = [host, info];
    this.div = document.createElement(info.type || !info.label ? 'div' : 'fieldset');
    this.div.id = this.id;
    host.settings.add(this);
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
    }
    this.input.value = this.getStringProperty(['bot', 'default']);
    this.setEnabled();
    this.div.appendChild(this.input);
  }

  setEnabled(enabled?: boolean) {
    if (enabled === !this.div.classList.contains('disabled')) return;
    if (enabled === undefined) enabled = this.enabled;
    if (!this.input) return;
    const { settings, view } = this.host;
    this.div.classList.toggle('disabled', !enabled);
    if (enabled && !this.input.value) this.input.value = this.getStringProperty(['bot', 'default', 'schema']);
    this.setProperty(enabled ? this.inputValue : undefined);
    settings.forEach(el => el.setVisibility());
    if (this.toggle) this.toggle.checked = enabled;
    if (this.info.radioGroup && enabled)
      view.querySelectorAll(`[data-radio-group="${this.info.radioGroup}"]`).forEach(el => {
        const setting = settings.byEl(el);
        if (setting === this) return;
        setting?.setEnabled(false);
      });
    // TODO, when a radio group is enabled as a result of another setting, we need to pick one
  }

  setVisibility() {
    const hide =
      Array.isArray(this.info.require) && this.info.require.some(id => !this.host.settings.byId[id].enabled);
    this.div.classList.toggle('none', hide);
    if (hide) this.setProperty(undefined);
  }

  update(_?: Event) {
    this.setProperty(this.inputValue);
    this.setEnabled();
  }

  select() {}

  get inputValue(): number | string | undefined {
    return this.input?.value;
  }

  get id() {
    return this.info.id!;
  }

  get enabled() {
    return this.getProperty() !== undefined;
  }

  setProperty(value: string | number | Mapping | undefined) {
    const path = objectPath(this.id);
    if (value === undefined) removePath({ obj: this.host.bot, path });
    else setPath({ obj: this.host.bot, path, value });
  }

  getProperty(sel: ObjectSelector[] = ['bot']) {
    for (const s of sel) {
      if (s === 'schema' && getSchemaDefault(this.id) !== undefined) return getSchemaDefault(this.id);
      const obj = s === 'bot' ? this.host.bot : this.host.botDefault;
      const prop = objectPath(this.id)?.reduce((o, key) => o?.[key], obj);
      if (prop !== undefined) return prop;
    }
    return undefined;
  }

  getStringProperty(sel: ObjectSelector[] = ['bot']) {
    const prop = this.getProperty(sel);
    return typeof prop === 'object' ? JSON.stringify(prop) : prop ? String(prop) : '';
  }
}

export class MappingSetting extends SettingNode {
  info: MappingInfo;
  edit: HTMLElement;
  constructor(p: NodeArgs) {
    super(p);
    this.init();
  }
  select() {
    console.log('select');
    this.edit = this.host.view.querySelector('.edit-panel') as HTMLElement;
    this.div.classList.add('selected');
    const maybeFrozen = this.getProperty(['bot', 'default', 'schema']) as Mapping;
    const mapping = maybeFrozen ? structuredClone(maybeFrozen) : maybeFrozen;
    this.setProperty(mapping);
    this.info.value = mapping;
    this.edit.innerHTML = `<span><button class="button">Moves</button><button class="button">Score</button></span><div class="chart-wrapper"><canvas></canvas></div>`;
    const canvas = this.edit.querySelector('canvas') as HTMLCanvasElement;
    canvas.innerHTML = '';
    renderMapping(canvas, this.info, () => this.host.bot.update());
  }
}

class SelectSetting extends SettingNode {
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
    this.init();
  }
  get inputValue(): string {
    return this.input.value;
  }
}

class TextSetting extends SettingNode {
  input = $as<HTMLInputElement>('<input type="text" data-type="string">');
  constructor(p: NodeArgs) {
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
  constructor(p: NodeArgs) {
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
  constructor(p: NodeArgs) {
    super(p);
    this.init();
    this.input.min = String(this.info.min);
    this.input.max = String(this.info.max);
    this.input.maxLength = maxChars(this.info);
    this.input.style.maxWidth = `calc(${maxChars(this.info)}ch + 1.5em)`;
    if (this.info.min === this.info.max) this.input.disabled = true;
  }
  update() {
    this.input.classList.toggle('invalid', !this.isValid());
    this.setProperty(this.isValid() ? this.inputValue : undefined);
    this.setEnabled();
  }
  isValid(el: HTMLInputElement = this.input) {
    const v = Number(el.value);
    return !isNaN(v) && v >= this.info.min && v <= this.info.max;
  }
  get inputValue(): number | undefined {
    return this.isValid() ? Number(this.input.value) : undefined;
  }
}

class RangeSetting extends NumberSetting {
  rangeInput = $as<HTMLInputElement>('<input type="range" data-type="number">');
  info: RangeInfo;
  constructor(p: NodeArgs) {
    super(p);
    this.rangeInput.min = String(this.info.min);
    this.rangeInput.max = String(this.info.max);
    this.rangeInput.step = String(this.info.step);
    this.rangeInput.value = this.input.value;
    this.div.insertBefore(this.rangeInput, this.input);
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
    this.setEnabled();
  }
}

type ObjectSelector = 'bot' | 'default' | 'schema';

type NodeArgs = { host: SettingHost; info: BaseInfo };

function buildSetting(host: SettingHost, info: BaseInfo) {
  const p = { host, info };
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
    case 'mapping':
      return new MappingSetting(p);
    default:
      return new SettingNode(p);
  }
}

function objectPath(id: string) {
  return id.split('_').slice(1);
}

function removePath({ obj, path }: { obj: any; path: string[] }) {
  if (!obj) return;
  if (path.length > 1) removePath({ obj: obj[path[0]], path: path.slice(1) });
  if (typeof obj[path[0]] !== 'object' || path.length === 1 || Object.keys(obj[path[0]]).length === 0)
    delete obj[path[0]];
}

function setPath({ obj, path, value }: { obj: any; path: string[]; value: any }) {
  if (path.length === 0) return;
  if (path.length === 1) obj[path[0]] = value;
  else if (!(path[0] in obj)) obj[path[0]] = {};
  setPath({ obj: obj[path[0]], path: path.slice(1), value });
}

function maxChars(info: NumberInfo | RangeInfo) {
  const len = Math.max(info.max.toString().length, info.min.toString().length);
  if (!('step' in info)) return len;
  const fractionLen = info.step < 1 ? String(info.step).length - String(info.step).indexOf('.') - 1 : 0;
  return len + fractionLen;
}
