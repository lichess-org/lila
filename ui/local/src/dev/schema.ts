import type { Schema, InfoKey, PropertyValue, Requirement } from './types';
import { deepFreeze } from 'common';

// give dialog constraints, describe dialog content, define direct mappings to BotInfo instances

export const infoKeys: InfoKey[] = [
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
  'template',
  'requires',
  'required',
]; // keep in sync with InfoKey in file://./types.ts

export const operatorRegex: RegExp = /==|>=|>|<=|<|!=/;

export const schema: Schema = deepFreeze<Schema>({
  bot_name: {
    type: 'text',
    class: ['setting'],
    value: 'bot name',
    required: true,
  },
  bot_description: {
    type: 'textarea',
    rows: 3,
    class: ['placard'],
    value: 'short description',
    required: true,
  },
  sources: {
    class: ['sources'],
    books: {
      label: 'books',
      type: 'books',
      class: ['books'],
      template: {
        min: { weight: 0 },
        max: { weight: 10 },
        step: { weight: 1 },
        value: { weight: 1 },
      },
      required: true,
    },
    sounds: {
      label: 'sounds',
      type: 'sounds',
      class: ['sound-events'],
      template: {
        min: { chance: 0, delay: 0, mix: 0 },
        max: { chance: 100, delay: 10, mix: 1 },
        step: { chance: 0.1, delay: 0.1, mix: 0.01 },
        value: { chance: 100, delay: 1, mix: 0.5 },
      },
      greeting: { label: 'greeting', class: ['sound-event'], type: 'soundEvent' },
      playerWin: { label: 'player win', class: ['sound-event'], type: 'soundEvent' },
      botWin: { label: 'bot win', class: ['sound-event'], type: 'soundEvent' },
      playerCheck: { label: 'player check', class: ['sound-event'], type: 'soundEvent' },
      botCheck: { label: 'bot check', class: ['sound-event'], type: 'soundEvent' },
      playerCapture: { label: 'player capture', class: ['sound-event'], type: 'soundEvent' },
      botCapture: { label: 'bot capture', class: ['sound-event'], type: 'soundEvent' },
      playerMove: { label: 'player move', class: ['sound-event'], type: 'soundEvent' },
      botMove: { label: 'bot move', class: ['sound-event'], type: 'soundEvent' },
      required: true,
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
    lc0bias: {
      label: 'lc0 bias',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 1 }, from: 'move', data: [] },
      requires: { and: ['sources_zero', 'sources_fish'] },
      required: true,
    },
    acplMean: {
      label: 'acpl mean',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 150 }, from: 'score', data: [] },
      requires: { and: ['sources_fish', 'sources_fish_multipv > 1'] },
    },
    acplStdev: {
      label: 'acpl stdev',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 100 }, from: 'score', data: [] },
      requires: 'bot_operators_acplMean',
      required: true,
    },
    decay: {
      label: 'line probability decay',
      type: 'operator',
      class: ['operator'],
      value: { range: { min: 0, max: 1 }, from: 'time', data: [] },
      requires: {
        or: [
          { and: ['sources_zero', 'sources_fish'] },
          { and: ['sources_fish', 'sources_fish_multipv > 1'] },
          { and: ['sources_zero', 'sources_zero_multipv > 1'] },
        ],
      },
    },
  },
});

export function getSchemaDefault(id: string): PropertyValue {
  const setting = schema[id] ?? id.split('_').reduce((obj, key) => obj[key], schema);
  return typeof setting === 'object' && 'value' in setting ? structuredClone(setting.value) : undefined;
}
