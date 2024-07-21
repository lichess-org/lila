import { deepFreeze } from 'common';
import type { Schema, AnyKey, PropertyValue } from './types';

export const primitiveKeys: AnyKey[] = [
  'type',
  'id',
  'label',
  'value',
  'assetType',
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

export const operatorRegex: RegExp = /==|>=|>|<=|<|!=/;

export const schema: Schema = {
  bot_name: {
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
  sources: {
    class: ['sources'],
    books: {
      label: 'books',
      class: ['books'],
      type: 'books',
      required: true,
    },
    sounds: {
      label: 'sounds',
      class: ['sounds'],
      type: 'group',
      required: true,
      greeting: {
        label: 'greeting',
        type: 'sounds',
        value: { chance: 100, volume: 1, delay: 0 },
      },
      playerWin: {
        label: 'player win',
        type: 'sounds',
        value: { chance: 100, volume: 1, delay: 2 },
      },
      botWin: {
        label: 'bot win',
        type: 'sounds',
        value: { chance: 100, volume: 1, delay: 1 },
      },
      playerCheck: {
        label: 'player check',
        type: 'sounds',
        value: { chance: 50, volume: 0.8, delay: 2 },
      },
      botCheck: {
        label: 'bot check',
        type: 'sounds',
        value: { chance: 50, volume: 0.8, delay: 1 },
      },
      playerCapture: {
        label: 'player capture',
        type: 'sounds',
        value: { chance: 30, volume: 0.8, delay: 2 },
      },
      botCapture: {
        label: 'bot capture',
        type: 'sounds',
        value: { chance: 30, volume: 0.8, delay: 1 },
      },
      playerMove: {
        label: 'player move',
        type: 'sounds',
        value: { chance: 2, volume: 0.8, delay: 3 },
      },
      botMove: {
        label: 'bot move',
        type: 'sounds',
        value: { chance: 2, volume: 0.8, delay: 2 },
      },
    },
    zero: {
      label: 'lc0',
      type: 'group',
      net: {
        label: 'model',
        type: 'select',
        class: ['setting'],
        assetType: 'net',
        required: true,
      },
      multipv: {
        label: 'lines',
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
        label: 'lines',
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
  bot_operators: {
    class: ['operators'],
    lc0line: {
      label: 'lc0 line',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 1 }, from: 'move', data: [] },
      requires: ['sources_zero_multipv > 1'],
      required: true,
    },
    lc0bias: {
      label: 'lc0 bias',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 1 }, from: 'move', data: [] },
      requires: ['sources_zero', 'sources_fish'],
      required: true,
    },
    acplMean: {
      label: 'acpl mean',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 150 }, from: 'score', data: [] },
      requires: ['sources_fish_multipv > 1'],
      required: true,
    },
    acplStdev: {
      label: 'acpl stdev',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 100 }, from: 'score', data: [] },
      requires: ['bot_operators_acplMean'],
      required: true,
    },
  },
};

deepFreeze(schema);

export function getSchemaDefault(id: string): PropertyValue {
  const setting = schema[id] ?? id.split('_').reduce((obj, key) => obj[key], schema);
  return typeof setting === 'object' && 'value' in setting ? structuredClone(setting.value) : undefined;
}
