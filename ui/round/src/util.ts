import { VNodeData } from 'snabbdom';
import * as sg from 'shogiground/types';
import { parseSfen } from 'shogiops/sfen';
import { shogigroundDests, shogigroundDropDests } from 'shogiops/compat';

export { bind, onInsert } from 'common/snabbdom';

export function justIcon(icon: string): VNodeData {
  return {
    attrs: { 'data-icon': icon },
  };
}

export function getMoveDests(sfen: string, variant: VariantKey): sg.Dests {
  return parseSfen(variant, sfen, false).unwrap(
    p => shogigroundDests(p),
    _ => new Map()
  ) as sg.Dests;
}

export function getDropDests(sfen: string, variant: VariantKey): sg.DropDests {
  return parseSfen(variant, sfen, false).unwrap(
    p => shogigroundDropDests(p),
    _ => new Map()
  ) as sg.DropDests;
}
