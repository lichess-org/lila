import { initialSfen } from 'shogiops/sfen';
import { defined } from './common';
import { variantToId } from './variant';

export function analysis(
  variant: VariantKey,
  sfen: Sfen,
  usis?: string[],
  color?: Color,
  moveNumber?: number,
  evaluation?: boolean,
): string {
  const variantPath = variant !== 'standard' ? `/${variant}` : '';
  const sfenPath = sfen === initialSfen(variant) ? '' : `/${encodeSfen(sfen)}`;
  const hash = defined(moveNumber) ? `#${moveNumber}` : '';

  return (
    window.lishogi.xhr.urlWithParams(`/analysis${variantPath}${sfenPath}`, {
      moves: usis?.length ? usis.join('_') : undefined,
      evaluation,
      color: color !== 'sente' ? color : undefined,
    }) + hash
  );
}

export function editor(variant: VariantKey, sfen: Sfen, color?: Color): string {
  const variantPath = variant !== 'standard' ? `/${variant}` : '';
  const orientationQuery = color && color !== 'sente' ? `?orientation=${color}` : '';
  if (sfen === initialSfen(variant)) return `/editor${variantPath}${orientationQuery}`;
  return `/editor${variantPath}/${encodeSfen(sfen)}${orientationQuery}`;
}

export function setup(
  baseUrl: string,
  variant: VariantKey,
  sfen: Sfen,
  mode?: 'ai' | 'friend',
): string {
  const sfenQuery = sfen !== initialSfen(variant) ? `sfen=${encodeSfen(sfen, true)}&` : '';
  const variantQuery = `variant=${variantToId(variant)}`;
  const modeAnchor = mode ? `#${mode}` : '';
  return `${baseUrl}?${sfenQuery}${variantQuery}${modeAnchor}`;
}

export function encodeSfen(sfen: string, query = false): string {
  const encoded = encodeURIComponent(sfen).replace(/%20/g, '_').replace(/%2F/g, '/');
  if (query) return encoded;
  else return encoded.replace(/%2B/g, '+');
}
