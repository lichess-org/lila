import { pubsub } from 'lib/pubsub';
import * as xhr from './xhr';
import { isPlayerPlaying, isPlayerTurn } from 'lib/game/game';
import type { RoundData } from './interfaces';

export default class Server {
  scheduledCheck: Timeout | undefined;

  constructor(readonly getData: () => RoundData) {
    if (isPlayerPlaying(this.getData())) pubsub.on('socket.in.serverRestart', this.onServerRestart);
  }

  alive = (): void => {
    if (this.scheduledCheck) {
      clearTimeout(this.scheduledCheck);
      this.scheduledCheck = undefined;
    }
  };

  private onServerRestart = (): void => {
    const wait = (12 + Math.random() * 15) * 1000;
    this.scheduledCheck = setTimeout(this.checkForDesync, wait);
  };

  private checkForDesync = (): void => {
    const d = this.getData();
    if (d.game.player !== d.player.color) {
      xhr.reload(d).then(n => {
        if (isPlayerTurn(n)) site.reload('Server desync detected');
      }, this.onServerRestart);
    }
  };
}
