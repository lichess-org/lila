declare module 'ab' {
  import { MoveMetadata } from 'chessground/types';
  function init (round: unknown): unknown
  function move (round: unknown, meta: MoveMetadata): unknown
}
