import { escapeHtml } from 'common';

interface Info {
  type: 'select' | 'text' | 'textarea' | 'range' | 'number';
  label: string;
  id: string;
  value: string;
  title?: string;
  hasPanel?: boolean;
  required?: boolean;
  radioGroup?: string; // exactly one enabled element must be checked for each radioGroup
}
interface SelectInfo extends Info {
  type: 'select';
  choices: (string | { name: string; value: string })[];
}
interface TextInfo extends Info {
  type: 'text';
}
interface TextareaInfo extends Info {
  type: 'textarea';
}
interface RangeInfo extends Info {
  type: 'range';
  min: number;
  max: number;
  step: number;
}
interface NumberInfo extends Info {
  type: 'number';
  min: number;
  max: number;
}
type SettingInfo = SelectInfo | TextInfo | TextareaInfo | RangeInfo | NumberInfo;

function dataTypeOf(type: 'select' | 'text' | 'textarea' | 'range' | 'number') {
  return type === 'number' || type === 'range' ? 'number' : 'string';
}

export class SettingElement {
  div: HTMLElement;
  toggle: HTMLInputElement;
  input: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
  asChart: HTMLInputElement;
  label: HTMLLabelElement;
  preview: HTMLLabelElement;
  constructor(public info: SettingInfo) {
    const { label, id, value, radioGroup, required, hasPanel } = this.info;
    this.div = $as<HTMLElement>(`<div id="${id}"`);
    if (!required) {
      this.toggle = $as<HTMLInputElement>(`<input type="checkbox" class="toggle-enabled" title="Enabled"`);
      if (value) this.toggle.checked = true;
      this.div.appendChild(this.toggle);
    }
    this.div.appendChild((this.label = $as<HTMLLabelElement>(`<label>${label}`)));
    if (info.type === 'select') {
      this.input = $as<HTMLSelectElement>(`<select>`);
      for (const c of info.choices) {
        const option = document.createElement('option');
        option.value = typeof c === 'string' ? c : c.value;
        option.textContent = typeof c === 'string' ? c : c.name;
        if (option.value === value) option.selected = true;
      }
    } else if (info.type === 'textarea') {
      this.input = $as<HTMLTextAreaElement>(`<textarea>${value}`);
    } else if (info.type === 'range') {
      this.input = $as<HTMLInputElement>(
        `<input type="${info.type}" min="${info.min}" max="${info.max}" step="${info.step}"`,
      );
    } else if (info.type === 'number') {
      this.input = $as<HTMLInputElement>(`<input type="${info.type}" min="${info.min}" max="${info.max}"`);
    } else {
      this.input = $as<HTMLInputElement>(`<input type="${info.type}"`);
    }
    if (info.title) this.input.title = info.title;
    this.input.dataset.datatype = dataTypeOf(info.type);
    this.input.value = value;
    this.div.appendChild(this.input);
    if (!(value && required)) this.div.classList.add('disabled');
    if (hasPanel) this.div.classList.add('hasPanel');
    if (radioGroup) this.div.dataset.radioGroup = radioGroup;
  }
}

export function settingHtml(info: SettingInfo) {
  return new SettingElement(info).div.outerHTML;
}
