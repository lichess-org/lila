import { Notify } from './notify';
import * as s from './setting';

export * from './render';
export * from './setting';
export * from './handler';

export type NvuiContext = Readonly<{
  notify: Notify;
  moveStyle: s.Setting<s.MoveStyle>;
  pieceStyle: s.Setting<s.PieceStyle>;
  prefixStyle: s.Setting<s.PrefixStyle>;
  positionStyle: s.Setting<s.PositionStyle>;
  boardStyle: s.Setting<s.BoardStyle>;
}>;

export function makeContext<T extends NvuiContext>(
  ctx: Pick<T, Exclude<keyof T, keyof NvuiContext>>,
  redraw?: Redraw,
): T {
  return {
    notify: new Notify(redraw),
    moveStyle: s.styleSetting(),
    pieceStyle: s.pieceSetting(),
    prefixStyle: s.prefixSetting(),
    positionStyle: s.positionSetting(),
    boardStyle: s.boardSetting(),
    ...ctx,
  } as T;
}
