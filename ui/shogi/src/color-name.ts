import { i18n } from 'i18n';

const ColorName = {
  Lang: 0,
  SenteJP: 1,
  Sente: 2,
  Black: 3,
} as const;

const colorNamePref: number = parseInt(document.body.dataset.colorName || '0');

export function colorName(color: Color, isHandicap: boolean): string {
  return isHandicap ? handicapColorName(color) : standardColorName(color);
}

function standardColorName(color: Color): string {
  switch (colorNamePref) {
    case ColorName.SenteJP:
      return color === 'sente' ? '先手' : '後手';
    case ColorName.Sente:
      return color === 'sente' ? 'Sente' : 'Gote';
    case ColorName.Black:
      return color === 'sente' ? i18n('black') : i18n('white');
    default:
      return color === 'sente' ? i18n('sente') : i18n('gote');
  }
}

function handicapColorName(color: Color): string {
  switch (colorNamePref) {
    case ColorName.SenteJP:
      return color === 'sente' ? '下手' : '上手';
    case ColorName.Sente:
      return color === 'sente' ? 'Shitate' : 'Uwate';
    case ColorName.Black:
      return color === 'sente' ? i18n('black') : i18n('white');
    default:
      return color === 'sente' ? i18n('shitate') : i18n('uwate');
  }
}
