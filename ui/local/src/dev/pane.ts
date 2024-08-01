import { removeObjectProperty, setObjectProperty, maxChars } from './util';
import { getSchemaDefault, operatorRegex } from './schema';
import type {
  PaneArgs,
  SelectInfo,
  TextInfo,
  TextareaInfo,
  NumberInfo,
  RangeInfo,
  HostView,
  PaneInfo,
  ObjectSelector,
  PropertyValue,
  Requirement,
} from './types';

export class Pane<Info extends PaneInfo = PaneInfo> {
  input?: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
  label?: HTMLLabelElement;
  readonly host: HostView;
  readonly info: Info;
  readonly el: HTMLElement;
  readonly parent: Pane | undefined;
  enabledCheckbox?: HTMLInputElement;

  constructor(args: PaneArgs) {
    Object.assign(this, args);
    this.el = document.createElement(this.isFieldset ? 'fieldset' : 'div');
    this.el.id = this.id;
    this.info.class?.forEach(c => this.el.classList.add(c));
    this.host.ctrl.add(this);
    if (this.info.title) this.el.title = this.info.title;
    if (this.info.label) {
      this.label = $as<HTMLLabelElement>(`<label><span>${this.info.label}</span></label>`);
      if (this.info.class?.includes('setting')) this.el.appendChild(this.label);
      else {
        const header = document.createElement(this.isFieldset ? 'legend' : 'span');
        header.appendChild(this.label);
        this.el.appendChild(header);
      }
    }
    this.initEnabledCheckbox();
    if (!this.enabledCheckbox) return;
    this.enabledCheckbox.classList.add('toggle');
    this.enabledCheckbox.checked = this.isDefined;
    this.label?.prepend(this.enabledCheckbox);
  }

  setEnabled(enabled: boolean = this.canEnable()): boolean {
    if (this.input || this.enabledCheckbox) {
      const { ctrl: editor, view } = this.host;
      this.el.classList.toggle('disabled', !enabled);

      if (this.input && !this.input.value)
        this.input.value = this.getStringProperty(['bot', 'default', 'schema']);

      if (enabled) this.host.bot.disabled.delete(this.id);
      else this.host.bot.disabled.add(this.id);

      for (const kid of this.children) {
        kid.el.classList.toggle('none', !enabled);
        if (!enabled) continue;
        if (kid.isRequired) kid.update();
        else if (kid.info.type !== 'radioGroup') continue;
        const radios = Object.values(editor.byId).filter(x => x.radioGroup === kid.id);
        const active = radios?.find(x => x.enabled) ?? radios?.find(x => x.getProperty(['default']));
        if (active) active.update();
        else if (radios.length) radios[0].update();
      }
      if (this.enabledCheckbox) this.enabledCheckbox.checked = enabled;
      if (this.radioGroup && enabled)
        view.querySelectorAll(`[name="${this.radioGroup}"]`).forEach(el => {
          const kid = editor.byEl(el);
          if (kid === this) return;
          kid?.setEnabled(false);
        });
    }
    for (const r of this.host.ctrl.dependsOn(this.id)) r.setEnabled(enabled ? undefined : false);
    return enabled;
  }

  update(_?: Event): void {
    this.setProperty(this.paneValue);
    this.setEnabled(this.isDefined);
    this.host.update();
  }

  setProperty(value: PropertyValue): void {
    if (value === undefined) {
      if (this.paneValue) removeObjectProperty({ obj: this.host.bot, path: { id: this.id } });
    } else setObjectProperty({ obj: this.host.bot, path: { id: this.id }, value });
  }

  getProperty(sel: ObjectSelector[] = ['bot']): PropertyValue {
    for (const s of sel) {
      const prop =
        s === 'schema'
          ? getSchemaDefault(this.id)
          : this.path.reduce((o, key) => o?.[key], s === 'bot' ? this.host.bot : this.host.defaultBot);
      if (prop !== undefined) return s === 'bot' ? prop : structuredClone(prop);
    }
    return undefined;
  }

  getStringProperty(sel: ObjectSelector[] = ['bot']): string {
    const prop = this.getProperty(sel);
    return typeof prop === 'object' ? JSON.stringify(prop) : prop !== undefined ? String(prop) : '';
  }

  get paneValue(): PropertyValue {
    return this.input?.value;
  }

  get id(): string {
    return this.info.id!;
  }

  get enabled(): boolean {
    if (this.host.bot.disabled.has(this.id)) return false;
    const kids = this.children;
    if (!kids.length) return this.isDefined && this.requirementsAllow;
    return kids.every(x => x.enabled || !x.isRequired);
  }

  get requires(): string[] {
    return getIds(this.info.requires);
  }

  protected init(): void {
    this.setEnabled();
    if (this.input) this.el.appendChild(this.input);
  }

  protected canEnable(): boolean {
    const kids = this.children;
    if (this.input && !kids.length) return this.isDefined;
    return kids.every(x => x.enabled || !x.isRequired) && this.requirementsAllow;
  }

  protected get path(): string[] {
    return this.id.split('_').slice(1);
  }

  protected get radioGroup(): string | undefined {
    return this.parent?.info.type === 'radioGroup' ? this.parent.id : undefined;
  }

  protected get isFieldset(): boolean {
    return this.info.type === 'group' || this.info.type === 'books' || this.info.type === 'sounds';
  }

  protected get isDefined(): boolean {
    return this.getProperty() !== undefined;
  }
  protected get requirementsAllow(): boolean {
    return this.evaluate(this.info.requires);
  }

  protected get isRequired(): boolean {
    return this.info.required === undefined ? false : this.info.required;
  }

  private evaluate(requirement: Requirement | undefined): boolean {
    if (typeof requirement === 'string') {
      const req = requirement.trim();
      if (req.startsWith('!')) {
        const paneId = req.slice(1).trim();
        const pane = this.host.ctrl.byId[paneId];
        return pane ? !pane.enabled : true;
      }

      const op = req.match(operatorRegex)?.[0] as string;

      const [left, right] = req.split(op).map(x => x.trim());
      const maybeLeftPane = this.host.ctrl.byId[left];
      const maybeRightPane = this.host.ctrl.byId[right];
      const leftValue = maybeLeftPane ? maybeLeftPane.paneValue : left;
      const rightValue = maybeRightPane ? maybeRightPane.paneValue : right;

      switch (op) {
        case '==':
          return String(leftValue) === String(rightValue);
        case '!=':
          return String(leftValue) !== String(rightValue);
        case '>=':
          return Number(leftValue) >= Number(rightValue);
        case '>':
          return Number(leftValue) > Number(rightValue);
        case '<=':
          return Number(leftValue) <= Number(rightValue);
        case '<':
          return Number(leftValue) < Number(rightValue);
        case undefined:
          return maybeLeftPane?.enabled;
      }
    } else if (Array.isArray(requirement)) {
      return requirement.every(r => this.evaluate(r));
    } else if (typeof requirement === 'object') {
      if ('and' in requirement) {
        return requirement.and.every(r => this.evaluate(r));
      } else if ('or' in requirement) {
        return requirement.or.some(r => this.evaluate(r));
      }
    }
    return true;
  }

  protected get children(): Pane[] {
    if (!this.id) return [];
    return Object.keys(this.host.ctrl.byId)
      .filter(id => id.startsWith(this.id) && id.split('_').length === this.id.split('_').length + 1)
      .map(id => this.host.ctrl.byId[id]);
  }

  private initEnabledCheckbox() {
    if (this.radioGroup) {
      this.enabledCheckbox = $as<HTMLInputElement>(
        `<input type="radio" name="${this.radioGroup}" tabindex="-1">`,
      );
    } else if (
      !this.isRequired &&
      this.info.label &&
      this.info.type !== 'books' &&
      this.info.type !== 'soundEvent'
    ) {
      this.enabledCheckbox = $as<HTMLInputElement>(`<input type="checkbox">`);
    }
  }
}

export class SelectSetting extends Pane<SelectInfo> {
  input: HTMLSelectElement = $as<HTMLSelectElement>('<select data-type="string">');
  constructor(p: PaneArgs) {
    super(p);
    for (const c of this.choices) {
      const option = document.createElement('option');
      option.value = c.value;
      option.textContent = c.name;
      if (option.value === this.getStringProperty()) option.selected = true;
      this.input.appendChild(option);
    }
    this.el.appendChild(document.createElement('hr'));
    this.init();
  }
  get choices(): { name: string; value: string }[] {
    if (!this.info.assetType) return this.info.choices ?? [];
    return this.host.assetDb.all(this.info.assetType).map(x => ({ name: x, value: x }));
  }
  get paneValue(): string {
    return this.input.value;
  }
}

export class TextSetting extends Pane<TextInfo> {
  input: HTMLInputElement = $as<HTMLInputElement>('<input type="text" data-type="string">');
  constructor(p: PaneArgs) {
    super(p);
    this.init();
  }
  get paneValue(): string {
    return this.input.value;
  }
}

export class TextareaSetting extends Pane<TextareaInfo> {
  input: HTMLTextAreaElement = $as<HTMLTextAreaElement>('<textarea data-type="string">');
  constructor(p: PaneArgs) {
    super(p);
    this.init();
    if (this.info.rows) this.input.rows = this.info.rows;
  }
  get paneValue(): string {
    return this.input.value;
  }
}

export class NumberSetting<Info extends NumberInfo = NumberInfo> extends Pane<Info> {
  input: HTMLInputElement = $as<HTMLInputElement>('<input type="text" data-type="number">');
  constructor(p: PaneArgs) {
    super(p);
    this.el.appendChild(document.createElement('hr'));
    this.init();
    this.input.maxLength = maxChars(this.info);
    this.input.style.maxWidth = `calc(${maxChars(this.info)}ch + 1.5em)`;
    if (this.info.min === this.info.max) this.input.disabled = true;
  }
  update(): void {
    const isValid = this.isValid();
    this.input.classList.toggle('invalid', !isValid);
    if (isValid) {
      this.setProperty(this.paneValue);
      this.setEnabled(true);
    }
  }
  isValid(el: HTMLInputElement = this.input): boolean {
    const v = Number(el.value);
    return !isNaN(v) && v >= this.info.min && v <= this.info.max;
  }
  get paneValue(): number | undefined {
    return this.isValid() ? Number(this.input.value) : undefined;
  }
}

export class RangeSetting<Info extends RangeInfo = RangeInfo> extends NumberSetting<Info> {
  rangeInput: HTMLInputElement = $as<HTMLInputElement>('<input type="range" data-type="number">');
  constructor(p: PaneArgs) {
    super(p);
    this.rangeInput.min = String(this.info.min);
    this.rangeInput.max = String(this.info.max);
    this.rangeInput.step = String(this.info.step);
    this.rangeInput.value = this.input.value;
    this.el.querySelector('hr')?.replaceWith(this.rangeInput);
  }
  update(e?: Event): void {
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

function getIds(r: Requirement | undefined): string[] {
  if (typeof r === 'string') {
    return [r.split(operatorRegex)[0].trim()];
  } else if (Array.isArray(r)) {
    return r.flatMap(getIds);
  } else if (typeof r === 'object' && r !== null) {
    if ('and' in r) return r.and.flatMap(getIds);
    if ('or' in r) return r.or.flatMap(getIds);
  }
  return [];
}
