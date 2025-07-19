import type { Schema, InfoKey, PropertyValue } from './devTypes';
import { deepFreeze } from 'lib/algo';

// describe dialog content, define constraints, maps to Bot instance data

export const infoKeys: InfoKey[] = [
  'type',
  'id',
  'label',
  'value',
  'placeholder',
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
  'toggle',
]; // InfoKey in file://./devTypes.ts

export const requiresOpRe: RegExp = /==|>=|>|<<=|<=|<|!=/; // <<= means startsWith

export const schema: Schema = deepFreeze<Schema>({
  info: {
    description: {
      type: 'textarea',
      rows: 3,
      class: ['placard'],
      placeholder: 'short description',
    },
    name: {
      type: 'text',
      label: 'name',
      class: ['setting'],
      placeholder: 'bot name',
    },
    ratings: {
      label: 'advertised rating',
      type: 'group',
      toggle: false,
      ultraBullet: {
        label: 'ultra bullet',
        type: 'range',
        class: ['setting'],
        value: 1500,
        min: 600,
        max: 2400,
        step: 10,
        toggle: true,
      },
      bullet: {
        label: 'bullet',
        type: 'range',
        class: ['setting'],
        value: 1500,
        min: 600,
        max: 2400,
        step: 10,
        toggle: true,
      },
      blitz: {
        label: 'blitz',
        type: 'range',
        class: ['setting'],
        value: 1500,
        min: 600,
        max: 2400,
        step: 10,
        toggle: true,
      },
      rapid: {
        label: 'rapid',
        type: 'range',
        class: ['setting'],
        value: 1500,
        min: 600,
        max: 2400,
        step: 10,
        toggle: true,
      },
      classical: {
        label: 'classical',
        type: 'range',
        class: ['setting'],
        value: 1500,
        min: 600,
        max: 2400,
        step: 10,
        toggle: true,
      },
    },
  },
  behavior: {
    class: ['behavior'],
    books: {
      label: 'books',
      type: 'books',
      class: ['books'],
      template: {
        min: { weight: 0 },
        max: { weight: 100 },
        step: { weight: 1 },
        value: { weight: 1 },
      },
      title: condense(
        `opening books may be imported into the asset databas from pgns, studies, or polyglot files.
        
        once imported, you may add any number of different opening books to a bot. the
        weight of a book is used to choose one when multiple books offer moves for the
        same position. a book with a weight of 10 is 10 times more likely to be selected
        than one with a weight of 1. a weight of 0 will disable a book without removing it`,
      ),
    },
    sounds: {
      label: 'sounds',
      type: 'sounds',
      class: ['sound-events'],
      template: {
        min: { chance: 0, delay: 0, mix: 0 },
        max: { chance: 100, delay: 10, mix: 1 },
        step: { chance: 0.1, delay: 0.1, mix: 0.01 },
        value: { chance: 100, delay: 0, mix: 0.5 },
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
    },
    zero: {
      label: 'lc0',
      type: 'group',
      toggle: true,
      net: {
        label: 'model',
        type: 'select',
        class: ['setting'],
        assetType: 'net',
      },
      multipv: {
        label: 'lines',
        type: 'range',
        class: ['setting'],
        value: 1,
        min: 1,
        max: 8,
        step: 1,
      },
      nodes: {
        label: 'nodes',
        type: 'range',
        class: ['setting'],
        value: 1,
        min: 1,
        max: 1000,
        step: 1,
        toggle: true,
        requires: {
          some: [
            'behavior_zero_net <<= e55a', // tinygyal
            'behavior_zero_net <<= 2d2e', // evilgyal
            'behavior_zero_net <<= d685', // goodgyal
            'behavior_zero_net <<= b5d2', // meangyal
          ],
        },
      },
    },
    fish: {
      label: 'stockfish',
      type: 'group',
      toggle: true,
      multipv: {
        label: 'lines',
        type: 'range',
        class: ['setting'],
        value: 12,
        min: 1,
        max: 20,
        step: 1,
      },
      depth: {
        label: 'depth',
        type: 'range',
        class: ['setting'],
        value: 10,
        min: 1,
        max: 14,
        step: 1,
      },
    },
  },
  bot_filters: {
    class: ['filters'],
    lc0bias: {
      label: 'lc0 bias',
      type: 'filter',
      class: ['filter'],
      value: { range: { min: 0, max: 1 }, by: 'max' },
      requires: { every: ['behavior_zero', 'behavior_fish'] },
      title: condense(
        `this filter assigns a weight in order to bias moves from the lc0 engine.
        a higher weight makes a move more likely to be selected.
        
        if the engine is configured to return multiple lines, the same bias is
        applied to every move, but the order lc0 prefers them is still respected.
        
        generally, moves are preferred in descending order of
        the sum of their weights. so lc0 bias can compensate for (by
        adding to) weights from other filters`,
      ),
    },
    cplTarget: {
      label: 'cpl target',
      type: 'filter',
      class: ['filter'],
      value: { range: { min: 0, max: 150 }, by: 'max' },
      requires: { every: ['behavior_fish', 'behavior_fish_multipv > 1'] },
      title: condense(
        `cpl target assigns a weight calculated from the average centipawn loss relative
        to bestmove according to stockfish at the chosen depth.
        
        it identifies the mean of a folded normal distribution of target cpl values. 
        
        each turn, a randomized cpl target is chosen from this distribution.
        the distance from a move's
        actual cpl to this randomized target is converted to a weight between 0 and 1 with a
        sigmoid function.
        
        moves are sorted in descending order by the sum of their weights (lc0 bias and/or cpl).`,
      ),
    },
    cplStdev: {
      label: 'cpl stdev',
      type: 'filter',
      class: ['filter'],
      value: { range: { min: 0, max: 100 }, by: 'max' },
      requires: 'bot_filters_cplTarget',
      title: `cpl stdev, if given, describes the standard deviation of the folder normal
      distribution from which each move's random cpl target is chosen. if not given, it defaults
      to 50. cpl stdev participates in the cpl target
      weight calculation. it does not assign its own weight.`,
    },
    aggression: {
      label: 'aggression',
      type: 'filter',
      class: ['filter'],
      value: { range: { min: -1, max: 1 }, by: 'avg' },
      requires: {
        some: [
          'behavior_fish_multipv > 1',
          'behavior_zero_multipv > 1',
          { every: ['behavior_zero', 'behavior_fish'] },
        ],
      },
      title: condense(
        `aggression assigns weights to moves that remove opponent material from the board.

        a value of 1 will increase the likelihood of captures, 0 is neutral, and -1 will avoid
        captures.
        
        this one should be combined with other filters.`,
      ),
    },
    pawnStructure: {
      label: 'pawn structure',
      type: 'filter',
      class: ['filter'],
      value: { range: { min: 0, max: 1 }, by: 'avg' },
      requires: {
        some: [
          'behavior_fish_multipv > 1',
          'behavior_zero_multipv > 1',
          { every: ['behavior_zero', 'behavior_fish'] },
        ],
      },
      title:
        condense(`pawn structure assigns weights up to the graph value for pawns that support each other, control the center,
        and are not doubled or isolated.
        
        This filter assigns a weight between 0 and 1.`),
    },
    moveDecay: {
      label: 'move quality decay',
      type: 'filter',
      class: ['filter'],
      value: { range: { min: 0, max: 1 }, by: 'max' },
      requires: {
        some: [
          'behavior_fish_multipv > 1',
          'behavior_zero_multipv > 1',
          { every: ['behavior_zero', 'behavior_fish'] },
        ],
      },
      title: condense(
        `move quality decay is an optional final stage of move selection.

        if any previous filter assigns weights, they are first used to sort moves in
        descending order
        of the weight sums. when move quality decay is off or zero,
        the first move in that sort order is chosen. if move quality decay is non-zero,
        each move's quality weight is equal to that decay raised to the power of the move's
        sort order index (counting from zero). a random number between
        0 and the sum of all quality weights will then select the final move.

        for example, with a decay of 0.5, the first move has a 50% chance of being chosen,
        the second move 25%, the third 12.5%, and so on. with a decay of 1, all moves
        are equally likely (ultrabullet).
        
        move quality decay is engine independent and can be used to resolve between
        scored stockfish and unscored lc0 moves.
        it operates on the full list provided by engine behavior and pairs well
        with the think time facet and a crisp chardonnay.`,
      ),
    },
  },
});

export function getSchemaDefault(id: string): PropertyValue {
  const setting = schema[id] ?? id.split('_').reduce((obj, key) => obj[key], schema);
  return typeof setting === 'object' && 'value' in setting ? setting.value : undefined;
}

function condense(str: string): string {
  return str
    .replace(/\n[ \t]*\n[ \t]*/g, '\n\n')
    .replace(/\n[ \t]+/g, ' ')
    .trim();
}
