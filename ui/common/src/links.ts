import { initialSfen } from 'shogiops/sfen';
import { urlWithParams } from './xhr';
import { defined } from './common';

export function analysis(
  variant: VariantKey,
  sfen: Sfen,
  usis?: string[],
  color?: Color,
  moveNumber?: number,
  evaluation?: boolean
) {
  const variantPath = variant !== 'standard' ? `/${variant}` : '',
    sfenPath = sfen === initialSfen(variant) ? '' : `/${encodeSfen(sfen)}`,
    hash = defined(moveNumber) ? `#${moveNumber}` : '';

  return (
    urlWithParams(`/analysis${variantPath}${sfenPath}`, {
      moves: usis && usis.length ? usis.join('_') : undefined,
      evaluation,
      color: color !== 'sente' ? color : undefined,
    }) + hash
  );
}

export function editor(variant: VariantKey, sfen: Sfen, color?: Color): string {
  const variantPath = variant !== 'standard' ? `/${variant}` : '',
    orientationQuery = color && color !== 'sente' ? `?orientation=${color}` : '';
  if (sfen === initialSfen(variant)) return `/editor${variantPath}${orientationQuery}`;
  return `/editor${variantPath}/${encodeSfen(sfen)}` + orientationQuery;
}

export function setup(baseUrl: string, variant: VariantKey, sfen: Sfen, mode?: 'ai' | 'friend'): string {
  const sfenQuery = sfen !== initialSfen(variant) ? `sfen=${encodeSfen(sfen, true)}&` : '',
    variantQuery = `variant=${variantToId(variant)}`,
    modeAnchor = mode ? `#${mode}` : '';
  return baseUrl + '?' + sfenQuery + variantQuery + modeAnchor;
}

export function encodeSfen(sfen: string, query = false): string {
  const encoded = encodeURIComponent(sfen).replace(/%20/g, '_').replace(/%2F/g, '/');
  if (query) return encoded;
  else return encoded.replace(/%2B/g, '+');
}

function variantToId(variant: VariantKey): number {
  switch (variant) {
    case 'minishogi':
      return 2;
    case 'chushogi':
      return 3;
    case 'annanshogi':
      return 4;
    case 'kyotoshogi':
      return 5;
    case 'checkshogi':
      return 6;
    default:
      return 1;
  }
}
