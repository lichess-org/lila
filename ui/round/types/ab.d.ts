declare module 'ab' {
  import { MoveMetadata } from 'chessground-newchess1-mod/types';
  function init(round: unknown): void;
  function move(round: unknown, meta: MoveMetadata): void;
}
