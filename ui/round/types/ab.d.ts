declare module 'ab' {
  import { MoveMetadata } from 'chessground/types';
  function init(round: unknown): void;
  function move(round: unknown, meta: MoveMetadata): void;
}
