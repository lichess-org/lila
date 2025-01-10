import type { Result } from '@badrap/result';
import type * as sg from 'shogiground/types';
import { shogigroundDropDests, shogigroundMoveDests } from 'shogiops/compat';
import type { Position } from 'shogiops/variant/position';
import type { VNodeData } from 'snabbdom';

export { bind, onInsert } from 'common/snabbdom';

export function justIcon(icon: string): VNodeData {
  return {
    attrs: { 'data-icon': icon },
  };
}
export function getMoveDests(posRes: Result<Position>): sg.MoveDests {
  return posRes.unwrap(
    p => shogigroundMoveDests(p),
    _ => new Map(),
  );
}

export function getDropDests(posRes: Result<Position>): sg.DropDests {
  return posRes.unwrap(
    p => shogigroundDropDests(p),
    _ => new Map(),
  );
}
