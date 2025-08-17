declare module 'ab' {
  import { MoveMetadata } from '@lichess-org/chessground/types';
  import { Pubsub } from 'lib/pubsub';
  function init(round: unknown): void;
  function move(round: unknown, meta: MoveMetadata, emit: Pubsub['emit']): void;
}
