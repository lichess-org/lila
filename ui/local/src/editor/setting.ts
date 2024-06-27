import type { PaneArgs, SelectInfo, TextareaInfo, NumberInfo, RangeInfo, ObjectSelector } from './types';
import type { Mapping } from '../types';
import { Pane } from './pane';
import { removePath, setPath, maxChars } from './util';
import { getSchemaDefault } from './schema';

export class Setting extends Pane {
  input?: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
  label?: HTMLElement;

  constructor(args: PaneArgs) {
    super(args);
    if (args.autoEnable) this.autoEnable = args.autoEnable;
    if (this.info.label) {
      this.label = document.createElement(this.div.tagName === 'FIELDSET' ? 'legend' : 'label');
      this.label.textContent = this.info.label;
      this.div.appendChild(this.label);
    }
    if (this.radioGroup) {
      this.enabledCheckbox = $as<HTMLInputElement>(
        `<input type="radio" name="${this.radioGroup}" tabindex="-1">`,
      );
    } else if (!this.info.required && this.label) {
      this.enabledCheckbox = $as<HTMLInputElement>(`<input type="checkbox">`);
    }
    if (this.enabledCheckbox) {
      this.enabledCheckbox.classList.add('toggle-enabled');
      this.enabledCheckbox.checked = this.getProperty() !== undefined; // ?
      if (this.label) this.label.prepend(this.enabledCheckbox);
      else this.div.prepend(this.enabledCheckbox as HTMLInputElement);
    }
  }

  init() {
    this.setEnabled();
    if (this.input) this.div.appendChild(this.input);
  }

  setEnabled(enabled = this.autoEnable()) {
    if (!this.input && !this.enabledCheckbox) return;

    const { editor, view } = this.host;
    this.div.classList.toggle('disabled', !enabled);

    if (this.input && !this.input.value)
      this.input.value = this.getStringProperty(['bot', 'default', 'schema']);

    if (enabled) this.host.bot.disabled.delete(this.id);
    else this.host.bot.disabled.add(this.id);

    for (const kid of this.children) {
      kid.div.classList.toggle('none', !enabled);
      if (!enabled) continue;
      if (kid.info.required) kid.update();
      else if (kid.info.type !== 'radio') continue;
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

  get paneValue(): number | string | Mapping | undefined {
    return this.input?.value;
  }

  autoEnable(): boolean {
    if (this.input) return this.enabled;
    for (const c of this.children) {
      if (c.info.required && c.getProperty() === undefined) return false;
    }
    for (const r of this.requires) {
      if (!this.host.editor.byId[r].enabled) return false;
    }
    return true;
  }
}

export class DisclosureSetting extends Setting {
  constructor(p: PaneArgs) {
    super(p);
    this.init();
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
    this.div.appendChild(document.createElement('hr'));
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
