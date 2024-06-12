import { Libot } from './types';

export const botSchema: BotSchema = {
  bot_name: {
    label: 'name',
    type: 'text',
    value: 'bot name',
    include: true,
  },
  bot_description: {
    type: 'textarea',
    rows: 3,
    value: 'short description',
    include: true,
  },
  bot: {
    class: 'settings',
    book: {
      label: 'book',
      type: 'select',
      value: undefined,
      choices: [],
    },
    zero: {
      label: 'Lc0',
      netName: {
        label: 'model',
        type: 'select',
        value: undefined,
        choices: [],
      },
      search: {
        include: (b: Libot) => b.zero?.netName !== undefined && !b.zero.netName.startsWith('maia'),
        depth: {
          label: 'depth',
          type: 'number',
          value: 1,
          min: 1,
          max: 20,
          radioGroup: 'zeroSearch',
        },
        nodes: {
          label: 'nodes',
          type: 'number',
          value: '100000',
          min: 1,
          max: 1000000,
          radioGroup: 'zeroSearch',
        },
        movetime: {
          label: 'movetime',
          type: 'number',
          value: '100',
          min: 1,
          max: 1000,
          radioGroup: 'zeroSearch',
        },
      },
    },
    fish: {
      label: 'Stockfish',
      multipv: {
        label: 'multipv',
        type: 'range',
        value: '12',
        min: 1,
        max: 50,
        step: 1,
      },
      search: {
        include: (b: Libot) => b.fish?.multipv !== undefined,
        depth: {
          label: 'depth',
          type: 'number',
          value: '12',
          min: 1,
          max: 20,
          radioGroup: 'fishSearch',
        },
        nodes: {
          label: 'nodes',
          type: 'number',
          value: '100000',
          min: 1,
          max: 1000000,
          radioGroup: 'fishSearch',
        },
        movetime: {
          label: 'movetime',
          type: 'number',
          value: '100',
          min: 1,
          max: 1000,
          radioGroup: 'fishSearch',
        },
      },
    },
  },
};

export function getSchemaDefault(id: string) {
  const setting = id.split('_').reduce((obj, key) => obj[key], botSchema) as SettingBaseInfo;
  return setting.value;
}

export function setSchemaBookChoices(books: string[]) {
  const prop = (botSchema as any).bot.book;
  prop.choices = books.map(book => ({ name: book, value: book }));
  prop.value = books[0];
}

export function setSchemaNetChoices(nets: { [name: string]: number }) {
  const prop = (botSchema as any).bot.zero.netName;
  prop.choices = Object.entries(nets).map(([name, value]) => ({ name, value }));
  prop.value = Object.keys(nets)[0];
}

export function dataTypeOf(type: SettingType): DataType {
  return type === 'number' || type === 'range' ? 'number' : 'string';
}

export type SettingInfo = SelectInfo | TextInfo | TextareaInfo | RangeInfo | NumberInfo;

export type Filter = (b: Libot) => boolean;

export interface BotSchema extends BaseInfo {
  [key: string]: SettingInfo | BotSchema | PropertyVal;
  type?: undefined;
}

function deepFreeze(obj: any) {
  for (const prop of Object.values(obj)) {
    if (prop && typeof prop === 'object' && !('choices' in prop)) deepFreeze(prop);
  }
  return Object.freeze(obj);
}

type SettingType = 'select' | 'text' | 'textarea' | 'range' | 'number';
type DataType = 'string' | 'number';
type PropertyVal = Filter | string | number | boolean | any[] | undefined;

interface BaseInfo {
  class?: string;
  label?: string;
  title?: string;
  include?: boolean | ((bot: Libot) => boolean);
}

interface SettingBaseInfo extends BaseInfo {
  type: SettingType;
  id?: string;
  hasPanel?: boolean;
  radioGroup?: string;
  value?: string | number | boolean;
}

interface SelectInfo extends SettingBaseInfo {
  type: 'select';
  value: string | undefined;
  choices: (string | { name: string; value: string })[];
}

interface TextInfo extends SettingBaseInfo {
  type: 'text';
}

interface TextareaInfo extends SettingBaseInfo {
  type: 'textarea';
  rows?: number;
}

interface RangeInfo extends SettingBaseInfo {
  type: 'range';
  min: number;
  max: number;
  step: number;
}

interface NumberInfo extends SettingBaseInfo {
  type: 'number';
  min: number;
  max: number;
}

deepFreeze(botSchema);
/*
function makeGroup(info: BaseInfo) {
  return new Group(info);
}
class Group {
  div: HTMLElement;
  label?: HTMLElement;
  //toggle?: HTMLInputElement;
  constructor(readonly info: BaseInfo) {
    this.div = $as<HTMLElement>(info.label ? `<fieldset>` : `<div>`);
    if (info.class) this.div.classList.add(info.class);
    if (info.title) this.div.title = info.title;
    if (info.label) this.div.appendChild((this.label = $as<HTMLElement>(`<label>${info.label}`)));
    /*if (info.include === undefined) {
      this.toggle = $as<HTMLInputElement>(`<input type="checkbox" class="toggle-enabled" title="Enabled">`);
      this.div.appendChild(this.toggle);
    }
  }
}

class Select extends Group {
  input: HTMLSelectElement;
  toggle?: HTMLInputElement;
  constructor(info: SelectInfo) {
    super(info);
    this.input = $as<HTMLSelectElement>(`<select>`);
    for (const c of info.choices) {
      const option = document.createElement('option');
      option.value = typeof c === 'string' ? c : c.name;
      option.textContent = typeof c === 'string' ? c : c.name;
      if (option.value === info.value) option.selected = true;
      this.input.appendChild(option);
    }
    this.div.appendChild(this.input);
  }
}

class Text extends Group {
  input: HTMLInputElement;
  constructor(info: TextInfo) {
    super(info);
    this.input = $as<HTMLInputElement>(`<input type="text">`);
    this.input.value = info.value ? String(info.value) : '';
    this.div.appendChild(this.input);
  }
}
  */
