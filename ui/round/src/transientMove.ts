import { RoundSocket } from './socket';

/* Tracks moves that were played on the board,
 * sent to the server, possibly acked,
 * but without a move response from the server yet.
 * After a delay, it will trigger a reload.
 * This might fix bugs where the board is in a
 * transient, dirty state, where clocks don't tick,
 * eventually causing the player to flag.
 * It will also help with lila-ws restarts.
 */
export default class TransientMove {
  constructor(readonly socket: RoundSocket) {}

  defaultTimeout = 7500;
  current: number | undefined = undefined;

  register = (remaining: number | undefined) => {
    const toTrigger = remaining && remaining <= this.defaultTimeout + 1000 ? 3500 : this.defaultTimeout;
    this.current = setTimeout(this.expire, toTrigger);
  };

  clear = () => {
    if (this.current) clearTimeout(this.current);
  };

  expire = () => {
    console.log('Server did not ack your move, we will try reloading the game.');
    this.socket.reload({});
  };
}
