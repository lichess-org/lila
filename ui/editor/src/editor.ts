import { prop, Prop } from 'common';
import { EditorConfig, EditorData, Castles, CASTLING_SIDES } from './interfaces';

export function init(cfg: EditorConfig): EditorData {
  return {
    color: prop(cfg.color),
    castles: {
      K: prop(cfg.castles.K),
      Q: prop(cfg.castles.Q),
      k: prop(cfg.castles.k),
      q: prop(cfg.castles.q),
    },
    baseUrl: cfg.baseUrl,
    positions: cfg.positions,
    variant: 'standard',
    i18n: cfg.i18n
  };
}

export function castlesAt(v: boolean): Castles<Prop<boolean>> {
  return {
    K: prop(v),
    Q: prop(v),
    k: prop(v),
    q: prop(v),
  };
}

function fenMetadatas(data: EditorData): string {
  let castles = '';
  for (const side of CASTLING_SIDES) {
    if (data.castles[side]()) castles += side;
  }
  return `${data.color()} ${castles.length ? castles : '-'} -`;
}

export function computeFen(data: EditorData, cgFen: string): string {
  return `${cgFen} ${fenMetadatas(data)}`;
}

export function makeUrl(url: string, fen: string): string {
  return url + encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
}
