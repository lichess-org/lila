import type { Schema, AnyKey, SelectInfo } from './types';
import { deepFreeze } from 'common';

export const primitiveKeys: AnyKey[] = [
  'type',
  'id',
  'class',
  'label',
  'title',
  'required',
  'requires',
  'choices',
  'value',
  'min',
  'max',
  'step',
  'rows',
]; // these keys are reserved for schema types

export let schema: Schema = {
  bot_name: {
    label: 'name',
    type: 'textSetting',
    value: 'bot name',
    required: true,
  },
  bot_description: {
    type: 'textareaSetting',
    rows: 3,
    class: ['placard'],
    value: 'short description',
    required: true,
  },
  bot_image: {
    label: 'image',
    type: 'selectSetting',
    choices: [],
    value: undefined,
    required: true,
  },
  sources: {
    class: ['sources'],
    book: {
      label: 'book',
      type: 'selectSetting',
      choices: [],
      value: undefined,
    },
    zero: {
      label: 'lc0',
      net: {
        label: 'model',
        type: 'selectSetting',
        choices: [],
        value: undefined,
        required: true,
      },
      multipv: {
        label: 'multipv',
        type: 'rangeSetting',
        value: 1,
        min: 1,
        max: 8,
        step: 1,
        required: true,
      },
    },
    fish: {
      label: 'stockfish',
      multipv: {
        label: 'multipv',
        type: 'rangeSetting',
        value: 12,
        min: 1,
        max: 20,
        step: 1,
        required: true,
      },
      by: {
        type: 'radio',
        required: true,
        nodes: {
          label: 'nodes',
          type: 'rangeSetting',
          value: 100000,
          min: 0,
          max: 1000000,
          step: 10000,
        },
        depth: {
          label: 'depth',
          type: 'rangeSetting',
          value: 12,
          min: 1,
          max: 20,
          step: 1,
        },
        movetime: {
          label: 'movetime',
          type: 'rangeSetting',
          value: 100,
          min: 0,
          max: 1000,
          step: 10,
        },
      },
    },
  },
  panels: {
    class: ['panels'],
    selectors: {
      lc0: {
        type: 'selectorPanel',
        label: 'lc0',
        title: 'Chance from 0 to 1 of selecting Lc0 move. 1 is always Lc0',
        value: { range: { min: 0, max: 1 }, from: 'move', data: [] },
        requires: ['sources_zero', 'sources_fish'],
      },
      acpl: {
        type: 'selectorPanel',
        label: 'acpl',
        title:
          'Normal distribution with mean=acpl and stdev=acpl/4 gives target move score reduction from best move',
        value: { range: { min: 10, max: 150 }, from: 'score', data: [] },
        requires: ['sources_fish'],
      },
    },
  },
};

deepFreeze(schema);

export function getSchemaDefault(id: string) {
  const setting = schema[id] ?? id.split('_').reduce((obj, key) => obj[key], schema);
  return typeof setting === 'object' && 'value' in setting ? structuredClone(setting.value) : undefined;
}

export function setSchemaAssets(a: { nets: string[]; images: string[]; books: string[] }) {
  schema = structuredClone(schema);
  const setChoices = (id: string, choices: string[]) => {
    const setting = (schema[id] ?? id.split('_').reduce((obj, key) => obj[key], schema)) as SelectInfo;
    setting.choices = choices.map(c => ({ name: c, value: c }));
    setting.value = setting.choices[0].value;
  };
  setChoices('bot_image', a.images);
  setChoices('sources_book', a.books);
  setChoices('sources_zero_net', a.nets);
  deepFreeze(schema);
}
