import type { BotSchema, BaseInfo, SelectInfo } from './types';
import { deepFreeze } from 'common';

export let botSchema: BotSchema = {
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
  bot_image: {
    label: 'image',
    type: 'select',
    choices: [],
    value: undefined,
    require: true,
  },
  bot_editPanel: {
    class: ['edit-panel'],
  },
  bot: {
    class: ['settings'],
    book: {
      label: 'book',
      type: 'select',
      choices: [],
      value: undefined,
    },
    zero: {
      label: 'Lc0',
      require: ['bot_zero_net', 'zeroSearch'],
      net: {
        label: 'model',
        type: 'select',
        choices: [],
        value: undefined,
        require: true,
      },
      search: {
        nodes: {
          label: 'nodes',
          type: 'number',
          value: 1,
          min: 1,
          max: 1,
          radioGroup: 'zeroSearch',
        },
        depth: {
          label: 'depth',
          type: 'range',
          value: 1,
          min: 1,
          max: 5,
          step: 1,
          radioGroup: 'zeroSearch',
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          value: 100,
          min: 0,
          max: 1000,
          step: 10,
          radioGroup: 'zeroSearch',
        },
      },
    },
    fish: {
      label: 'Stockfish',
      require: ['bot_fish_multipv', 'fishSearch'],
      multipv: {
        label: 'multipv',
        type: 'range',
        value: 12,
        min: 1,
        max: 20,
        step: 1,
        require: true,
      },
      search: {
        nodes: {
          label: 'nodes',
          type: 'range',
          value: 100000,
          min: 0,
          max: 1000000,
          step: 10000,
          radioGroup: 'fishSearch',
        },
        depth: {
          label: 'depth',
          type: 'range',
          value: 12,
          min: 1,
          max: 20,
          step: 1,
          radioGroup: 'fishSearch',
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          value: 100,
          min: 0,
          max: 1000,
          step: 10,
          radioGroup: 'fishSearch',
        },
      },
    },
    searchMix: {
      type: 'mapping',
      label: 'Lc0 frequency',
      title: 'Chance from 0 to 1 of selecting Lc0 move. 1 is always Lc0',
      class: ['selectable'],
      value: { from: 'const', to: 'lc0', data: 0.5, range: { min: 0, max: 1 } },
      require: ['bot_zero', 'bot_fish'],
    },
    acpl: {
      type: 'mapping',
      label: 'ACPL',
      title:
        'Normal distribution with mean=acpl and stdev=acpl/4 gives target move score reduction from best move',
      class: ['selectable'],
      value: { from: 'const', to: 'acpl', data: 80, range: { min: 10, max: 150 } },
      require: ['bot_fish'],
    },
  },
};

export const reservedKeys: (keyof BaseInfo)[] = [
  'type',
  'id',
  'class',
  'label',
  'title',
  'require',
  'radioGroup',
  'value',
];

deepFreeze(botSchema);

export function getSchemaDefault(id: string) {
  const setting = botSchema[id] ?? id.split('_').reduce((obj, key) => obj[key], botSchema);
  return typeof setting === 'object' && 'value' in setting ? structuredClone(setting.value) : undefined;
}

export function setSchemaAssets(a: { nets: string[]; images: string[]; books: string[] }) {
  botSchema = structuredClone(botSchema);
  const setChoices = (id: string, choices: string[]) => {
    const setting = (botSchema[id] ?? id.split('_').reduce((obj, key) => obj[key], botSchema)) as SelectInfo;
    setting.choices = choices.map(c => ({ name: c, value: c }));
    setting.value = setting.choices[0].value;
  };
  setChoices('bot_image', a.images);
  setChoices('bot_book', a.books);
  setChoices('bot_zero_net', a.nets);
  deepFreeze(botSchema);
}
