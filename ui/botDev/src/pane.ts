import { removeObjectProperty, setObjectProperty, maxChars } from './devUtil';
import { frag } from 'lib';
import { getSchemaDefault, requiresOpRe } from './schema';
import type { EditDialog } from './editDialog';
import { env } from './devEnv';
import type {
  PaneArgs,
  SelectInfo,
  TextInfo,
  TextareaInfo,
  NumberInfo,
  RangeInfo,
  PaneInfo,
  PropertySource,
  PropertyValue,
  Requirement,
} from './devTypes';

export class Pane<Info extends PaneInfo = PaneInfo> {
  input?: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
  label?: HTMLLabelElement;
  toggle?: (v?: boolean) => boolean; // TODO use common whatever
  readonly info: Info;
  readonly host: EditDialog;
  readonly el: HTMLElement;
  readonly parent: Pane | undefined;

  constructor(args: PaneArgs) {
    Object.assign(this, args);
    this.el = document.createElement(this.isFieldset ? 'fieldset' : 'div');
    this.el.id = this.id;
    this.info.class?.forEach(c => this.el.classList.add(c));
    this.host.panes.add(this);
    if (this.info.title) this.el.title = this.info.title;
    if (this.info.label) {
      this.label = frag<HTMLLabelElement>(`<label><span>${this.info.label}</span></label>`);
      if (this.info.class?.includes('setting')) this.el.appendChild(this.label);
      else {
        const header = document.createElement(this.isFieldset ? 'legend' : 'span');
        header.appendChild(this.label);
        this.el.appendChild(header);
      }
    }
    const toggleInputEl = this.radioGroup
      ? frag<HTMLInputElement>(`<input type="radio" class="toggle" name="${this.radioGroup}" tabindex="-1">`)
      : this.isOptional && this.info.label && this.info.type !== 'books' && this.info.type !== 'soundEvent'
        ? frag<HTMLInputElement>(`<input type="checkbox" class="toggle">`)
        : undefined;
    if (!toggleInputEl) return;
    toggleInputEl.checked = this.isDefined;
    this.label?.prepend(toggleInputEl);
    this.toggle = (v?: boolean) => {
      if (v !== undefined) toggleInputEl.checked = v;
      return toggleInputEl.checked;
    };
  }

  setEnabled(enabled: boolean = this.canEnable): boolean {
    const allowed = this.requirementsAllow;
    if (!allowed) enabled = false;
    this.el.classList.toggle('none', !allowed);

    if (this.input || this.toggle) {
      const { panes: editor, view } = this.host;
      this.el.classList.toggle('disabled', !enabled);

      if (enabled) this.host.editing().disabled.delete(this.id);
      else this.host.editing().disabled.add(this.id);

      if (this.input && !this.input.value)
        this.input.value = this.getStringProperty(['scratch', 'local', 'server', 'schema']);

      for (const kid of this.children) {
        kid.el.classList.toggle('none', !enabled || !kid.requirementsAllow);
        if (!enabled) continue;
        if (!kid.isOptional) kid.update();
        else if (kid.info.type !== 'radioGroup') continue;
        const radios = Object.values(editor.byId).filter(x => x.radioGroup === kid.id);
        const active = radios?.find(x => x.enabled) ?? radios?.find(x => x.getProperty(['local', 'server']));
        if (active) active.update();
        else if (radios.length) radios[0].update();
      }

      this.toggle?.(enabled);
      if (this.radioGroup && enabled)
        view.querySelectorAll(`[name="${this.radioGroup}"]`).forEach(el => {
          const radio = editor.byEl(el);
          if (radio === this) return;
          radio?.setEnabled(false);
        });
    }
    for (const r of this.host.panes.dependsOn(this.id)) r.setEnabled();
    return enabled;
  }

  update(_?: Event): void {
    this.setProperty(this.paneValue);
    this.setEnabled(this.isDefined);
    this.host.update();
  }

  setProperty(value: PropertyValue): void {
    if (value === undefined) {
      if (this.paneValue) removeObjectProperty({ obj: this.host.editing(), path: { id: this.id } });
    } else setObjectProperty({ obj: this.host.editing(), path: { id: this.id }, value });
  }

  getProperty(from: PropertySource[] = ['scratch']): PropertyValue {
    return findMap(from, src =>
      src === 'schema'
        ? getSchemaDefault(this.id)
        : this.path.reduce(
            (o, key) => o?.[key],
            src === 'scratch'
              ? this.host.editing()
              : src === 'local'
                ? this.host.localBot
                : this.host.serverBot,
          ),
    );
  }

  getStringProperty(src: PropertySource[] = ['scratch']): string {
    const prop = this.getProperty(src);
    return typeof prop === 'object' ? JSON.stringify(prop) : prop !== undefined ? String(prop) : '';
  }

  get paneValue(): PropertyValue {
    return this.input?.value;
  }

  get id(): string {
    return this.info.id!;
  }

  get enabled(): boolean {
    if (this.isDisabled) return false;
    const kids = this.children;
    if (!kids.length) return this.isDefined && this.requirementsAllow;
    return kids.every(x => x.enabled || x.isOptional);
  }

  get requires(): string[] {
    return getRequirementIds(this.info.requires);
  }

  protected init(): void {
    this.setEnabled();
    if (this.input) this.el.appendChild(this.input);
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

  protected get isDisabled(): boolean {
    return this.host.editing().disabled.has(this.id) || (this.parent !== undefined && this.parent.isDisabled);
  }

  protected get children(): Pane[] {
    if (!this.id) return [];
    return Object.keys(this.host.panes.byId)
      .filter(id => id.startsWith(this.id) && id.split('_').length === this.id.split('_').length + 1)
      .map(id => this.host.panes.byId[id]);
  }

  protected get isOptional(): boolean {
    return this.info.toggle === true;
  }

  protected get requirementsAllow(): boolean {
    return !this.parent?.isDisabled && this.evaluate(this.info.requires);
  }

  protected get canEnable(): boolean {
    const kids = this.children;
    if (this.input && !kids.length) return this.isDefined;
    return kids.every(x => x.enabled || x.isOptional) && this.requirementsAllow;
  }

  private evaluate(requirement: Requirement | undefined): boolean {
    if (typeof requirement === 'string') {
      const req = requirement.trim();
      if (req.startsWith('!')) {
        const paneId = req.slice(1).trim();
        const pane = this.host.panes.byId[paneId];
        return pane ? !pane.enabled : true;
      }

      const op = req.match(requiresOpRe)?.[0] as string;
      const [left, right] = req.split(op).map(x => x.trim());

      if ([left, right].some(x => this.host.panes.byId[x]?.enabled === false)) return false;

      const maybeLeftPane = this.host.panes.byId[left];
      const maybeRightPane = this.host.panes.byId[right];
      const leftValue = maybeLeftPane ? maybeLeftPane.paneValue : left;
      const rightValue = maybeRightPane ? maybeRightPane.paneValue : right;

      switch (op) {
        case '==':
          return String(leftValue) === String(rightValue);
        case '!=':
          return String(leftValue) !== String(rightValue);
        case '<<=':
          return String(leftValue).startsWith(String(rightValue));
        case '>=':
          return Number(leftValue) >= Number(rightValue);
        case '>':
          return Number(leftValue) > Number(rightValue);
        case '<=':
          return Number(leftValue) <= Number(rightValue);
        case '<':
          return Number(leftValue) < Number(rightValue);
        default:
          return maybeLeftPane?.enabled;
      }
    } else if (Array.isArray(requirement)) {
      return requirement.every(r => this.evaluate(r));
    } else if (typeof requirement === 'object') {
      if ('every' in requirement) {
        return requirement.every.every(r => this.evaluate(r));
      } else if ('some' in requirement) {
        return requirement.some.some(r => this.evaluate(r));
      }
    }
    return true;
  }
}

export class SelectSetting extends Pane<SelectInfo> {
  input: HTMLSelectElement = frag<HTMLSelectElement>('<select data-type="string">');
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
    return [...env.assets.allKeyNames(this.info.assetType).entries()].map(([key, name]) => ({
      name,
      value: key,
    }));
  }
  get paneValue(): string {
    return this.input.value;
  }
}

export class TextSetting extends Pane<TextInfo> {
  input: HTMLInputElement = frag<HTMLInputElement>(
    '<input type="text" data-type="string" spellcheck="false">',
  );
  constructor(p: PaneArgs) {
    super(p);
    this.init();
    if (this.info.placeholder) this.input.setAttribute('placeholder', this.info.placeholder);
  }
  get paneValue(): string {
    return this.input.value ?? '';
  }
}

export class TextareaSetting extends Pane<TextareaInfo> {
  input: HTMLTextAreaElement = frag<HTMLTextAreaElement>('<textarea data-type="string" spellcheck="false">');
  constructor(p: PaneArgs) {
    super(p);
    this.init();
    if (this.info.placeholder) this.input.setAttribute('placeholder', this.info.placeholder);
  }
  get paneValue(): string {
    return this.input.value ?? '';
  }
}

export class NumberSetting<Info extends NumberInfo = NumberInfo> extends Pane<Info> {
  input: HTMLInputElement = frag<HTMLInputElement>('<input type="text" data-type="number">');
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
  rangeInput: HTMLInputElement = frag<HTMLInputElement>('<input type="range" data-type="number">');
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

function getRequirementIds(r: Requirement | undefined): string[] {
  if (typeof r === 'string') {
    const req = r.trim();
    if (req.startsWith('!')) return [`${req.slice(1).trim()}`];
    const [left, right] = req.split(requiresOpRe).map(x => x.trim());
    const ids = [];
    if (left && isNaN(Number(left))) ids.push(left);
    if (right && isNaN(Number(right))) ids.push(right);
    return ids;
  } else if (Array.isArray(r)) {
    return r.flatMap(getRequirementIds);
  } else if (typeof r === 'object' && r !== null) {
    if ('every' in r) return r.every.flatMap(getRequirementIds);
    if ('some' in r) return r.some.flatMap(getRequirementIds);
  }
  return [];
}

function findMap<T, U>(arr: T[], fn: (v: T) => U | undefined): U | undefined {
  for (const v of arr) {
    const result = fn(v);
    if (result !== undefined) return result;
  }
  return undefined;
}
