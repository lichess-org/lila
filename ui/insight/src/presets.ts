import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';
import { h } from 'snabbdom';
import Ctrl from './ctrl';
import { Preset } from './interfaces';

export default function (ctrl: Ctrl) {
  return h(
    'div.box.presets',
    (ctrl.ui.asMod ? modPresets : basePresets).map(p =>
      h(
        'a.preset.text',
        {
          class: { active: ctrl.makeUrl(p.dimension, p.metric, p.filters) === ctrl.makeCurrentUrl() },
          attrs: { 'data-icon': licon.Target },
          hook: bind('click', () => ctrl.setQuestion(p)),
        },
        p.name,
      ),
    ),
  );
}

const basePresets: Preset[] = [
  {
    name: 'Do I gain more rating points against weaker or stronger opponents?',
    dimension: 'opponentStrength',
    metric: 'ratingDiff',
    filters: {},
  },
  {
    name: 'How quickly do I move each piece in bullet and blitz games?',
    dimension: 'piece',
    metric: 'movetime',
    filters: {
      variant: ['bullet', 'blitz'],
    },
  },
  {
    name: 'What is the Win-Rate of my favourite openings as white?',
    dimension: 'openingVariation',
    metric: 'result',
    filters: {
      variant: ['bullet', 'blitz', 'rapid', 'classical', 'correspondence'],
      color: ['white'],
    },
  },
  {
    name: 'How often do I punish blunders made by my opponent during each game phase?',
    dimension: 'phase',
    metric: 'awareness',
    filters: {},
  },
  {
    name: "Do I gain rating when I don't castle kingside?",
    dimension: 'myCastling',
    metric: 'ratingDiff',
    filters: {
      myCastling: ['2', '3'],
    },
  },
  {
    name: 'When I trade queens, how do games end?',
    dimension: 'queenTrade',
    metric: 'result',
    filters: {
      queenTrade: ['true'],
    },
  },
  {
    name: 'What is the average rating of my opponents across each variant?',
    dimension: 'variant',
    metric: 'opponentRating',
    filters: {},
  },
  {
    name: 'How well do I move each piece in the opening?',
    dimension: 'piece',
    metric: 'accuracy',
    filters: {
      phase: ['1'],
    },
  },
];

const modPresets: Preset[] = [
  {
    name: 'ACPL by date',
    dimension: 'date',
    metric: 'acpl',
    filters: {},
  },
  {
    name: 'Blurs by date',
    dimension: 'date',
    metric: 'blurs',
    filters: {},
  },
  {
    name: 'ACPL by blur',
    dimension: 'blur',
    metric: 'acpl',
    filters: {},
  },
  {
    name: 'Blurs by result',
    dimension: 'result',
    metric: 'blurs',
    filters: {},
  },
  {
    name: 'ACPL by time variance',
    dimension: 'timeVariance',
    metric: 'acpl',
    filters: {},
  },
  {
    name: 'Blur by time variance',
    dimension: 'timeVariance',
    metric: 'blurs',
    filters: {},
  },
  {
    name: 'Time variance by date',
    dimension: 'date',
    metric: 'timeVariance',
    filters: {},
  },
];
