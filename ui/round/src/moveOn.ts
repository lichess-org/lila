import * as game from 'game';
import * as xhr from './xhr';
import RoundController from './ctrl';

export default class MoveOn {

  value: boolean = window.lidraughts.storage.get(this.key) === '1' || window.lidraughts.storage.get(this.key) === '2';
  seq: boolean = window.lidraughts.storage.get(this.key) === '2';

  constructor(private ctrl: RoundController, private key: string) { }

  store = () => window.lidraughts.storage.set(this.key, this.value ? (this.seq ? '2' : '1') : '0');

  toggle = (): boolean => {
    this.value = !this.value;
    this.store();
    this.next(true);
    return this.value;
  };

  toggleSeq = (): boolean => {
    this.seq = !this.seq;
    this.store();
    return this.seq;
  };

  get = () => this.value;
  getSeq = () => this.seq;

  set = (v: boolean) => {
    this.value = v;
    this.store();
  };

  private redirect = (href: string) => {
    this.ctrl.setRedirecting();
    window.lidraughts.hasToReload = true;
    window.location.href = href;
  };

  next = (force?: boolean, timeout?: boolean): void => {
    const d = this.ctrl.data;
    if (!this.value || d.player.spectator || !game.isSwitchable(d) || (game.isPlayerTurn(d) && !timeout)) return;
    if (force) this.redirect('/round-next/' + d.game.id);
    else if (d.simul) {
      if (d.simul.hostId === this.ctrl.opts.userId && d.simul.nbPlaying > 1) {
        if (this.seq) this.redirect('/round-next-seq/' + d.game.id);
        else this.redirect('/round-next/' + d.game.id);
      }
    } else xhr.whatsNext(this.ctrl).then(data => {
      if (data.next) this.redirect('/' + data.next);
    });
  };

  timeOutGame = (seconds: number): void => {
    if (this.ctrl.data.simul)
      xhr.timeOutGame(this.ctrl, this.ctrl.data.simul.id, seconds).then(res => {
        if (res.ok && seconds > 0) this.next(false, true);
      });
  };
}
