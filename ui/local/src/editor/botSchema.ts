import type { BotSchema, SelectInfo, BaseInfo } from './types';
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
      net: {
        label: 'model',
        type: 'select',
        choices: [],
        value: undefined,
      },
      search: {
        nodes: {
          label: 'nodes',
          type: 'number',
          value: 1,
          min: 1,
          max: 1,
          radioGroup: 'zeroSearch',
          require: ['bot_zero_net'],
        },
        depth: {
          label: 'depth',
          type: 'range',
          value: 1,
          min: 1,
          max: 5,
          step: 1,
          radioGroup: 'zeroSearch',
          require: ['bot_zero_net'],
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          value: 100,
          min: 0,
          max: 1000,
          step: 10,
          radioGroup: 'zeroSearch',
          require: ['bot_zero_net'],
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
          type: 'range',
          value: 1,
          min: 1,
          max: 1000001,
          step: 10000,
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
          min: 0,
          max: 1000,
          step: 10,
          radioGroup: 'fishSearch',
          require: ['bot_fish_multipv'],
        },
      },
    },
    searchMix: {
      type: 'mapping',
      label: 'Lc0 frequency',
      title:
        'Likelihood of selecting Lc0 move over previous head. 0 is always previous head, 1 is always Lc0',
      class: ['selectable'],
      value: { from: 'moves', to: 'mix', default: 0.5, data: [], range: { min: 0, max: 1 } },
      require: ['bot_zero_net', 'bot_fish_multipv'],
    },
    acpl: {
      type: 'mapping',
      label: 'ACPL target',
      title:
        'Normal distribution with a mean of acpl and stdev acpl/4 yields the target cp distance from the best move',
      class: ['selectable'],
      value: { from: 'score', to: 'acpl', default: 80, data: [], range: { min: 10, max: 150 } },
      require: ['bot_fish_multipv'],
    },
  },
};

botSchema = deepFreeze(botSchema);

export function getSchemaDefault(id: string) {
  const setting = botSchema[id] ?? id.split('_').reduce((obj, key) => obj[key], botSchema);
  const value = setting ? (setting as BaseInfo).value : undefined;
  return typeof value === 'object' ? structuredClone(value) : value;
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
