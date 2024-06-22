import type { BotSchema, AnyKey, SelectInfo } from './types';
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
  sources: {
    class: ['sources'],
    book: {
      label: 'book',
      type: 'select',
      choices: [],
      value: undefined,
    },
    zero: {
      label: 'Lc0',
      require: ['sources_zero_net', 'sources_zero_search'],
      net: {
        label: 'model',
        type: 'select',
        choices: [],
        value: undefined,
        require: true,
      },
      search: {
        type: 'radioGroup',
        nodes: {
          label: 'nodes',
          type: 'number',
          value: 1,
          min: 1,
          max: 1,
        },
        depth: {
          label: 'depth',
          type: 'range',
          value: 1,
          min: 1,
          max: 5,
          step: 1,
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          value: 100,
          min: 0,
          max: 1000,
          step: 10,
        },
      },
    },
    fish: {
      label: 'Stockfish',
      require: ['sources_fish_multipv', 'sources_fish_search'],
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
        type: 'radioGroup',
        nodes: {
          label: 'nodes',
          type: 'range',
          value: 100000,
          min: 0,
          max: 1000000,
          step: 10000,
        },
        depth: {
          label: 'depth',
          type: 'range',
          value: 12,
          min: 1,
          max: 20,
          step: 1,
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          value: 100,
          min: 0,
          max: 1000,
          step: 10,
        },
      },
    },
  },
  selectors: {
    class: ['selectors'],
    searchMix: {
      type: 'mapping',
      label: 'Lc0 frequency',
      title: 'Chance from 0 to 1 of selecting Lc0 move. 1 is always Lc0',
      class: ['selectable'],
      value: { from: 'const', to: 'lc0', data: 0.5, range: { min: 0, max: 1 } },
      require: ['sources_zero', 'sources_fish'],
    },
    acpl: {
      type: 'mapping',
      label: 'ACPL',
      title:
        'Normal distribution with mean=acpl and stdev=acpl/4 gives target move score reduction from best move',
      class: ['selectable'],
      value: { from: 'const', to: 'acpl', data: 80, range: { min: 10, max: 150 } },
      require: ['sources_fish'],
    },
  },
};

export const reservedKeys: AnyKey[] = [
  'type',
  'id',
  'class',
  'label',
  'title',
  'require',
  'choices',
  'value',
  'min',
  'max',
  'step',
  'rows',
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
  setChoices('sources_book', a.books);
  setChoices('sources_zero_net', a.nets);
  deepFreeze(botSchema);
}
