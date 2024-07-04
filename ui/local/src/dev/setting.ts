import type { PaneArgs, SelectInfo, TextareaInfo, NumberInfo, RangeInfo } from './types';
import type { Mapping, Book } from '../types';
import { Pane } from './pane';
import { removeObjectProperty, setObjectProperty, maxChars } from './util';
import { getSchemaDefault } from './schema';

export class Setting extends Pane {
  input?: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;

  constructor(args: PaneArgs) {
    super(args);
    if (this.info.label) {
      const label = $as<HTMLElement>(`<label>${this.info.label}</label>`);
      if (this.info.class?.includes('setting')) this.el.appendChild(label);
      else {
        const header = document.createElement(this.isFieldset ? 'legend' : 'span');
        //header.classList.add('header');
        header.appendChild(label);
        this.el.appendChild(header);
      }
    }
    if (this.radioGroup) {
      this.enabledCheckbox = $as<HTMLInputElement>(
        `<input type="radio" name="${this.radioGroup}" tabindex="-1">`,
      );
    } else if (!this.info.required && this.info.label) {
      this.enabledCheckbox = $as<HTMLInputElement>(`<input type="checkbox">`);
    }
    if (!this.enabledCheckbox) return;
    this.enabledCheckbox.classList.add('toggle');
    this.enabledCheckbox.checked = this.isDefined;
    this.el.prepend(this.enabledCheckbox);
    this.label?.prepend(this.enabledCheckbox);
    //else this.el.prepend(this.enabledCheckbox as HTMLInputElement);
  }

  init() {
    this.setEnabled();
    if (this.input) this.el.appendChild(this.input);
  }

  setEnabled(enabled = this.canEnable()) {
    //this.el.classList.toggle('disabled', !enabled);
    if (!this.input && !this.enabledCheckbox) return;

    const { editor, view } = this.host;
    this.el.classList.toggle('disabled', !enabled);

    if (this.input && !this.input.value)
      this.input.value = this.getStringProperty(['bot', 'default', 'schema']);

    if (enabled) this.host.bot.disabled.delete(this.id);
    else this.host.bot.disabled.add(this.id);

    for (const kid of this.children) {
      kid.el.classList.toggle('none', !enabled);
      if (!enabled) continue;
      if (kid.info.required) kid.update();
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
    for (const r of this.host.editor.requires(this.id)) r.setEnabled(enabled ? undefined : false);
  }

  canEnable(): boolean {
    const kids = this.children;
    if (this.input && !kids.length) return this.isDefined;
    return kids.every(x => x.enabled || !x.info.required) && this.requirementsAllow;
  }

  get label(): HTMLElement | undefined {
    if (this.info.label) return this.el.querySelector('label') ?? undefined;
    return undefined;
  }

  get paneValue(): number | string | Mapping | Book[] | undefined {
    return this.input?.value;
  }

  get header(): HTMLElement {
    return this.label?.parentElement as HTMLElement;
  }
}

export class SelectSetting extends Setting {
  input = $as<HTMLSelectElement>('<select data-type="string">');
  info: SelectInfo;
  constructor(p: PaneArgs) {
    super(p);
    for (const c of this.info.choices) {
      const option = document.createElement('option');
      option.value = c.value;
      option.textContent = c.name;
      if (option.value === this.getStringProperty()) option.selected = true;
      this.input.appendChild(option);
    }
    this.el.appendChild(document.createElement('hr'));
    this.init();
  }

  get paneValue(): string {
    return this.input.value;
  }
}

export class TextSetting extends Setting {
  input = $as<HTMLInputElement>('<input type="text" data-type="string">');
  constructor(p: PaneArgs) {
    super(p);
    this.init();
  }
  get paneValue(): string {
    return this.input.value;
  }
}

export class TextareaSetting extends Setting {
  input = $as<HTMLTextAreaElement>('<textarea data-type="string">');
  info: TextareaInfo;
  constructor(p: PaneArgs) {
    super(p);
    this.init();
    if (this.info.rows) this.input.rows = this.info.rows;
  }
  get paneValue(): string {
    return this.input.value;
  }
}

export class NumberSetting extends Setting {
  input = $as<HTMLInputElement>('<input type="text" data-type="number">');
  info: NumberInfo | RangeInfo;
  constructor(p: PaneArgs) {
    super(p);
    this.el.appendChild(document.createElement('hr'));
    this.init();
    this.input.maxLength = maxChars(this.info);
    this.input.style.maxWidth = `calc(${maxChars(this.info)}ch + 1.5em)`;
    if (this.info.min === this.info.max) this.input.disabled = true;
  }
  update() {
    const isValid = this.isValid();
    this.input.classList.toggle('invalid', !isValid);
    if (isValid) {
      this.setProperty(this.paneValue);
      this.setEnabled(true);
    }
  }
  isValid(el: HTMLInputElement = this.input) {
    const v = Number(el.value);
    return !isNaN(v) && v >= this.info.min && v <= this.info.max;
  }
  get paneValue(): number | undefined {
    return this.isValid() ? Number(this.input.value) : undefined;
  }
}

export class RangeSetting extends NumberSetting {
  rangeInput = $as<HTMLInputElement>('<input type="range" data-type="number">');
  info: RangeInfo;
  constructor(p: PaneArgs) {
    super(p);
    this.rangeInput.min = String(this.info.min);
    this.rangeInput.max = String(this.info.max);
    this.rangeInput.step = String(this.info.step);
    this.rangeInput.value = this.input.value;
    this.el.querySelector('hr')?.replaceWith(this.rangeInput);
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
