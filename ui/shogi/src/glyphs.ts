import { i18n } from 'i18n';

interface AllGlyphs {
  move: Tree.Glyph[];
  observation: Tree.Glyph[];
  position: Tree.Glyph[];
}

export const allGlyphs: AllGlyphs = {
  move: [
    {
      id: 1,
      symbol: '!',
      name: i18n('goodMove'),
    },
    {
      id: 2,
      symbol: '?',
      name: i18n('mistake'),
    },
    {
      id: 3,
      symbol: '!!',
      name: i18n('brilliantMove'),
    },
    {
      id: 4,
      symbol: '??',
      name: i18n('blunder'),
    },
    {
      id: 5,
      symbol: '!?',
      name: 'Interesting move',
    },
    {
      id: 6,
      symbol: '?!',
      name: 'Dubious move',
    },
    {
      id: 7,
      symbol: '□',
      name: 'Only move',
    },
    {
      id: 22,
      symbol: '⨀',
      name: 'Zugzwang',
    },
  ],
  position: [
    {
      id: 10,
      symbol: '=',
      name: 'Equal position',
    },
    {
      id: 13,
      symbol: '∞',
      name: 'Unclear position',
    },
    {
      id: 14,
      symbol: '⩲',
      name: 'Sente is slightly better',
    },
    {
      id: 15,
      symbol: '⩱',
      name: 'Gote is slightly better',
    },
    {
      id: 16,
      symbol: '±',
      name: 'Sente is better',
    },
    {
      id: 17,
      symbol: '∓',
      name: 'Gote is better',
    },
    {
      id: 18,
      symbol: '+−',
      name: 'Sente is winning',
    },
    {
      id: 19,
      symbol: '-+',
      name: 'Gote is winning',
    },
  ],
  observation: [
    {
      id: 146,
      symbol: 'N',
      name: 'Novelty',
    },
    {
      id: 32,
      symbol: '↑↑',
      name: 'Development',
    },
    {
      id: 36,
      symbol: '↑',
      name: 'Initiative',
    },
    {
      id: 40,
      symbol: '→',
      name: 'Attack',
    },
    {
      id: 132,
      symbol: '⇆',
      name: 'Counterplay',
    },
    {
      id: 138,
      symbol: '⊕',
      name: 'Time trouble',
    },
    {
      id: 44,
      symbol: '=∞',
      name: 'With compensation',
    },
    {
      id: 140,
      symbol: '∆',
      name: 'With the idea',
    },
  ],
};
