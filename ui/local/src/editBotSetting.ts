import { escapeHtml } from 'common';
import { EditBotDialog } from './editBotDialog';
import { botSchema, SettingInfo, getSchemaDefault } from './editBotSchema';

export function makeEditView(dlg: EditBotDialog) {
  const el = $as<HTMLElement>(`<div class="edit-bot">`);
  const playerInfo = $as<HTMLElement>(`<div class="player-info">`);
  playerInfo.appendChild(makeElement(dlg, botSchema, ['bot_name']));
  playerInfo.appendChild(
    $as<HTMLElement>(`<div class="player ${dlg.color}"><div class="placard ${dlg.color}">Player</div></div>`),
  );
  playerInfo.appendChild(makeElement(dlg, botSchema, ['bot_description']));
  el.appendChild(playerInfo);
  el.appendChild(makeElement(dlg, botSchema, ['bot']));
  el.appendChild($as<HTMLElement>('<div class="edit-panel"><canvas></canvas></div>'));
  return el;
}

export class SettingElement {
  div: HTMLElement;
  toggle: HTMLInputElement;
  input: HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement;
  asChart: HTMLInputElement;
  label: HTMLLabelElement;
  preview: HTMLLabelElement;

  constructor(
    readonly dlg: EditBotDialog,
    public info: SettingInfo,
  ) {
    const { label, id, value, radioGroup, hasPanel } = this.info;
    this.div = $as<HTMLElement>(`<div id="${id}">`);
    this.div.classList.add('setting');
    const include = typeof info.include === 'boolean' ? info.include : info.include?.(dlg.bot);
    if (include === undefined) {
      const attrs = radioGroup ? `type="radio" name="${radioGroup}" tabindex="-1"` : 'type="checkbox"';
      this.toggle = $as<HTMLInputElement>(`<input ${attrs} class="toggle-enabled" title="Enabled">`);
      if (value) this.toggle.checked = true;
      this.div.appendChild(this.toggle);
    }
    if (label) this.div.appendChild((this.label = $as<HTMLLabelElement>(`<label>${escapeHtml(label)}`)));
    if (info.type === 'select') {
      this.input = $as<HTMLSelectElement>(`<select>`);
      for (const c of info.choices) {
        const option = document.createElement('option');
        option.value = typeof c === 'string' ? c : c.name;
        option.textContent = typeof c === 'string' ? c : c.name;
        console.log('select:', value, option.value, c);
        if (option.value === value) {
          option.selected = true;
        }
        this.input.appendChild(option);
      }
    } else if (info.type === 'textarea') {
      this.input = $as<HTMLTextAreaElement>(
        `<textarea${info.rows ? ` rows="${info.rows}"` : ''}>${value}</textarea>`,
      );
    } else if (info.type === 'range') {
      this.input = $as<HTMLInputElement>(
        `<input type="${info.type}" min="${info.min}" max="${info.max}" step="${info.step}">`,
      );
    } else if (info.type === 'number') {
      this.input = $as<HTMLInputElement>(`<input type="${info.type}" min="${info.min}" max="${info.max}">`);
    } else {
      this.input = $as<HTMLInputElement>(`<input type="${info.type}">`);
    }
    if (info.title) this.input.title = info.title;
    this.input.dataset.type = dataTypeOf(info.type);
    this.input.value = value ? String(value) : '';
    this.div.appendChild(this.input);
    if (info.type === 'range') {
      this.preview = $as<HTMLLabelElement>(`<label>${value}</label`);
      this.div.appendChild(this.preview);
    }
    if (!value && !include) this.div.classList.add('disabled');
    if (hasPanel) this.div.classList.add('hasPanel');
    if (radioGroup) this.div.dataset.radioGroup = radioGroup;
  }

  update() {
    if (this.info.type === 'range') this.preview.textContent = this.value;
    //else if (this.info.type === 'select') this.dlg.selectBot();
  }

  get id() {
    return this.info.id;
  }

  get value() {
    return this.input.value;
  }

  set value(value: string) {
    this.input.value = value;
  }

  set enabled(enabled: boolean) {
    if (enabled) {
      if (!this.input.value) this.input.value = String(getSchemaDefault(this.id!));
      this.div.classList.remove('disabled');
    } else {
      this.div.classList.add('disabled');
      this.input.value = '';
    }
    if (this.toggle) this.toggle.checked = enabled;
  }
}

function makeElement(dlg: EditBotDialog, schema: any, path: string[] = []) {
  const iter = path.reduce((acc, key) => acc[key], schema);
  if (iter.type && path.length) {
    const id = path.join('_');
    dlg.els[id] = new SettingElement(dlg, { ...iter, id, value: dlg.getProperty(id) });
    if (iter.include !== true && iter.include?.(dlg.bot) === false) dlg.els[id].div.classList.add('none');
    return dlg.els[id].div;
  }
  const group = document.createElement(iter.label ? 'fieldset' : 'div');
  if (iter.label) group.innerHTML = `<legend>${iter.label}</legend>`;
  if (iter.class) group.classList.add(iter.class);
  if (iter.id) group.id = iter.id;
  for (const key in iter) {
    if (!['id', 'type', 'label', 'include', 'class'].includes(key)) {
      group.appendChild(makeElement(dlg, schema, [...path, key]));
    }
  }
  if (iter.include !== true && iter.include?.(dlg.bot) === false) group.classList.add('none');
  return group;
}

function dataTypeOf(type: 'select' | 'text' | 'textarea' | 'range' | 'number') {
  return type === 'number' || type === 'range' ? 'number' : 'string';
}

export type Settings = { [id: string]: SettingElement };
