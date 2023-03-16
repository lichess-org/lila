import { isHandicap } from './handicaps';

export const enum ColorName {
  Lang,
  SenteJP,
  Sente,
  Black,
}

const colorNamePref = parseInt(document.body.dataset.colorName || '0');

export function transWithColorName(trans: Trans, key: I18nKey, color: Color, initialSfen: Sfen | undefined): string {
  const res = trans(key, colorName(trans.noarg, color, initialSfen));
  return capitalizeFirstLetter(res);
}

export function colorName(noarg: TransNoArg, color: Color, initialSfen: Sfen | undefined): string {
  const handicap = initialSfen && isHandicap(initialSfen);
  return handicap ? handicapColorName(noarg, color) : standardColorName(noarg, color);
}

export function standardColorName(noarg: TransNoArg, color: Color): string {
  switch (colorNamePref) {
    case ColorName.SenteJP:
      return color === 'sente' ? '先手' : '後手';
    case ColorName.Sente:
      return color === 'sente' ? 'Sente' : 'Gote';
    case ColorName.Black:
      return color === 'sente' ? noarg('black') : noarg('white');
    default:
      return color === 'sente' ? noarg('sente') : noarg('gote');
  }
}

export function handicapColorName(noarg: TransNoArg, color: Color): string {
  switch (colorNamePref) {
    case ColorName.SenteJP:
      return color === 'sente' ? '下手' : '上手';
    case ColorName.Sente:
      return color === 'sente' ? 'Shitate' : 'Uwate';
    case ColorName.Black:
      return color === 'sente' ? noarg('black') : noarg('white');
    default:
      return color === 'sente' ? noarg('shitate') : noarg('uwate');
  }
}

function capitalizeFirstLetter(str: string) {
  return str.charAt(0).toLocaleUpperCase() + str.slice(1).toLocaleLowerCase();
}
