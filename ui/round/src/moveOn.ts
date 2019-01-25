import * as game from 'game';
import * as xhr from './xhr';
import RoundController from './ctrl';

export default class MoveOn {

  value: boolean = window.lichess.storage.get(this.key) === '1';

  constructor(private ctrl: RoundController, private key: string) {
  }

  store = () => window.lichess.storage.set(this.key, this.value ? '1' : '0');

  toggle = (): boolean => {
    this.value = !this.value;
    this.store();
    this.next(true);
    return this.value;
  };

  get = () => this.value;

  set = (v: boolean) => {
    this.value = v;
    this.store();
  };

  private redirect = (href: string) => {
    this.ctrl.setRedirecting();
    window.lichess.hasToReload = true;
    window.location.href = href;
  };

  next = (force?: boolean): void => {
    const d = this.ctrl.data;
    if (!this.value || d.player.spectator || !game.isSwitchable(d) || game.isPlayerTurn(d)) return;
    if (force) this.redirect('/round-next/' + d.game.id);
    else if (d.simul) {
      if (d.simul.hostId === this.ctrl.opts.userId && d.simul.nbPlaying > 1)
        this.redirect('/round-next/' + d.game.id);
    } else xhr.whatsNext(this.ctrl).then(data => {
      if (data.next) this.redirect('/' + data.next);
    });
  };
}
