declare module 'ab' {
  import { MoveMetadata } from 'chessground/types';
  import { Pubsub } from 'common/pubsub';
  function init(round: unknown): void;
  function move(round: unknown, meta: MoveMetadata, emit: Pubsub['emit']): void;
}
