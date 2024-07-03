import type { Schema, AnyKey, SelectInfo } from './types';
import { deepFreeze } from 'common';

export const primitiveKeys: AnyKey[] = [
  'type',
  'id',
  'label',
  'value',
  'class',
  'choices',
  'title',
  'min',
  'max',
  'step',
  'rows',
  'requires',
  'required',
]; // these keys are reserved

export let schema: Schema = {
  bot_name: {
    //label: 'name',
    type: 'text',
    class: ['setting'],
    value: 'bot name',
    required: true,
  },
  bot_description: {
    type: 'textarea',
    rows: 3,
    class: ['placard', 'setting'],
    value: 'short description',
    required: true,
  },
  bot_image: {
    label: 'image',
    type: 'select',
    class: ['setting'],
    choices: [],
    value: undefined,
    required: true,
  },
  sources: {
    class: ['sources'],
    books: {
      label: 'books',
      class: ['books'],
      type: 'books',
      choices: [],
      value: [],
      required: true,
      min: 0,
      max: 99,
    },
    zero: {
      label: 'lc0',
      type: 'group',
      net: {
        label: 'model',
        type: 'select',
        class: ['setting'],
        choices: [],
        value: undefined,
        required: true,
      },
      multipv: {
        label: 'multipv',
        type: 'range',
        class: ['setting'],
        value: 1,
        min: 1,
        max: 8,
        step: 1,
        required: true,
      },
    },
    fish: {
      label: 'stockfish',
      type: 'group',
      multipv: {
        label: 'multipv',
        type: 'range',
        class: ['setting'],
        value: 12,
        min: 1,
        max: 20,
        step: 1,
        required: true,
      },
      by: {
        type: 'radioGroup',
        required: true,
        depth: {
          label: 'depth',
          type: 'range',
          class: ['setting'],
          value: 12,
          min: 1,
          max: 20,
          step: 1,
        },
        movetime: {
          label: 'movetime',
          type: 'range',
          class: ['setting'],
          value: 100,
          min: 0,
          max: 1000,
          step: 10,
        },
        nodes: {
          label: 'nodes',
          type: 'range',
          class: ['setting'],
          value: 100000,
          min: 0,
          max: 1000000,
          step: 10000,
        },
      },
    },
  },
  bot_selectors: {
    class: ['selectors'],
    lc0Bias: {
      label: 'lc0 bias',
      type: 'moveSelector',
      class: ['move-selector'],
      value: { range: { min: 0, max: 1 }, from: 'move', data: [] },
      requires: ['sources_zero', 'sources_fish'],
      required: true,
    },
    acplMean: {
      label: 'acpl mean',
      type: 'moveSelector',
      class: ['move-selector'],
      value: { range: { min: 0, max: 150 }, from: 'score', data: [] },
      requires: ['sources_fish'],
    },
    acplStdev: {
      label: 'acpl stdev',
      type: 'moveSelector',
      class: ['move-selector'],
      value: { range: { min: 0, max: 100 }, from: 'score', data: [] },
      requires: ['bot_selectors_acplMean'],
      required: true,
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
  setChoices('sources_books', a.books);
  setChoices('sources_zero_net', a.nets);
  deepFreeze(schema);
}
