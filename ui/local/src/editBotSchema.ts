import { BotInfo } from './types';
import { EditBotDialog } from './editBotDialog';
import { buildSetting } from './editBotSetting';

export const botSchema: BotSchema = {
  bot_name: {
    label: 'name',
    type: 'text',
    value: 'bot name',
    require: true,
  },
  bot_description: {
    type: 'textarea',
    rows: 3,
    class: ['placard'],
    value: 'short description',
    require: true,
  },
  bot: {
    class: ['settings'],
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
        nodes: {
          label: 'nodes',
          type: 'number',
          value: 1,
          min: 1,
          max: 99999,
          radioGroup: 'zeroSearch',
          require: ['bot_zero_netName'],
        },
        depth: {
          label: 'depth',
          type: 'range',
          value: 1,
          min: 1,
          max: 5,
          step: 1,
          radioGroup: 'zeroSearch',
          require: ['bot_zero_netName'],
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          value: 100,
          min: 1,
          max: 999,
          step: 10,
          radioGroup: 'zeroSearch',
          require: ['bot_zero_netName'],
        },
      },
    },
    fish: {
      label: 'Stockfish',
      multipv: {
        label: 'multipv',
        type: 'range',
        value: 12,
        min: 1,
        max: 50,
        step: 1,
      },
      search: {
        nodes: {
          label: 'nodes',
          type: 'number',
          value: 1,
          min: 1,
          max: 999999,
          radioGroup: 'fishSearch',
          require: ['bot_fish_multipv'],
        },
        depth: {
          label: 'depth',
          type: 'range',
          value: 12,
          min: 1,
          max: 20,
          step: 1,
          radioGroup: 'fishSearch',
          require: ['bot_fish_multipv'],
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          value: 100,
          min: 1,
          max: 999,
          step: 10,
          radioGroup: 'fishSearch',
          require: ['bot_fish_multipv'],
        },
      },
    },
  },
};

export function getSchemaDefault(id: string) {
  const setting = botSchema[id] ?? id.split('_').reduce((obj, key) => obj[key], botSchema);
  return (setting as BaseInfo)?.value;
}

export function setSchemaBookChoices(books: { name: string; value: any }[]) {
  const prop = (botSchema as any).bot.book;
  prop.choices = books.map(book => ({ name: book.name, value: JSON.stringify(book.value) }));
  prop.value = books[0];
}

export function setSchemaNetChoices(nets: { [name: string]: number }) {
  const prop = (botSchema as any).bot.zero.netName;
  prop.choices = Object.entries(nets).map(([name, value]) => ({ name, value }));
  prop.value = Object.keys(nets)[0];
}

export interface BotSchema extends BaseInfo {
  [key: string]: SelectInfo | TextInfo | TextareaInfo | RangeInfo | NumberInfo | BotSchema | PropertyVal;
  type?: undefined;
}

type PropertyVal = string | number | boolean | any[] | undefined; // TODO fix any[]

export interface BaseInfo {
  type?: 'select' | 'text' | 'textarea' | 'range' | 'number';
  id?: string;
  class?: string[];
  label?: string;
  title?: string;
  require?: string[] | boolean;
  radioGroup?: string;
  hasPanel?: boolean;
  value?: string | number | boolean;
}

export interface SelectInfo extends BaseInfo {
  type: 'select';
  value: string | undefined;
  choices: { name: string; value: string }[];
}

interface TextInfo extends BaseInfo {
  type: 'text';
  value: string;
}

export interface TextareaInfo extends BaseInfo {
  type: 'textarea';
  value: string;
  rows?: number;
}

export interface RangeInfo extends BaseInfo {
  type: 'range';
  value: number;
  min: number;
  max: number;
  step: number;
}

export interface NumberInfo extends BaseInfo {
  type: 'number';
  value: number;
  min: number;
  max: number;
}

export function buildFromSchema(dlg: EditBotDialog, path: string[] = []) {
  const iter = path.reduce<any>((acc, key) => acc[key], botSchema);
  const s = buildSetting({ id: path.join('_'), ...iter }, dlg);
  if (iter?.type) return s;
  for (const key of Object.keys(iter).filter(k => !reserved.includes(k as keyof BaseInfo))) {
    s.div.appendChild(buildFromSchema(dlg, [...path, key]).div);
  }
  return s;
}

const reserved: (keyof BaseInfo)[] = [
  'type',
  'id',
  'class',
  'label',
  'title',
  'require',
  'radioGroup',
  'hasPanel',
  'value',
];

function deepFreeze(obj: any) {
  for (const prop of Object.values(obj)) {
    if (prop && typeof prop === 'object' && !('choices' in prop)) deepFreeze(prop);
  }
  return Object.freeze(obj);
}

deepFreeze(botSchema);
