import { isSwitchable, isPlayerTurn } from 'game';
import { whatsNext } from './xhr';
import type RoundController from './ctrl';
import { storage } from 'common/storage';

export default class MoveOn {
  private storage = storage.boolean(this.key);

  constructor(
    private ctrl: RoundController,
    private key: string,
  ) {}

  toggle = (): void => {
    this.storage.toggle();
    this.next(true);
  };

  get: () => boolean = this.storage.get;

  private redirect = (href: string) => {
    this.ctrl.setRedirecting();
    window.location.href = href;
  };

  next = (force?: boolean): void => {
    const d = this.ctrl.data;
    if (d.player.spectator || !isSwitchable(d) || isPlayerTurn(d) || !this.get()) return;
    if (force) this.redirect('/round-next/' + d.game.id);
    else if (d.simul) {
      if (d.simul.hostId === this.ctrl.opts.userId && d.simul.nbPlaying > 1)
        this.redirect('/round-next/' + d.game.id);
    } else
      whatsNext(this.ctrl).then(data => {
        if (data.next) this.redirect('/' + data.next);
      });
  };
}
