import { makeSocket, SimulSocket } from './socket';
import xhr from './xhr';
import simul from './simul';
import * as status from 'game/status';
import {
  SimulData,
  SimulOpts
} from './interfaces';

export default class SimulCtrl {

  data: SimulData;
  trans: Trans;
  socket: SimulSocket;

  constructor(readonly opts: SimulOpts, readonly redraw: () => void) {
    this.trans = window.lichess.trans(opts.i18n);
    this.socket = makeSocket(opts.socketSend, this);
    if (simul.createdByMe(this) && this.data.isCreated)
      window.lichess.storage.set('lichess.move_on', '1'); // hideous hack :D
    this.hostPing();
  }

  reload = (data: SimulData) => {
    this.data = {
      ...data,
      team: this.data.team // reload data does not contain the team anymore
    }
  };

  teamBlock = () => this.data.team && !this.data.team.isIn;

  hostPing = () => {
    if (this.createdByMe() && this.data.isCreated) {
      xhr.ping(this);
      setTimeout(this.hostPing, 10000);
    }
  }

  createdByMe = () => this.opts.userId === this.data.host.id;
  candidates = () => this.data.applicants.filter(a => !a.accepted);
  accepted = () => this.data.applicants.filter(a => a.accepted);
  myCurrentPairing = () =>
    this.opts.userId ? this.data.pairings.find(p =>
      p.game.status < status.ids.mate && p.player.id === this.opts.userId
    ) : null;
  acceptedContainsMe = () => this.accepted().some(a => a.player.id === this.opts.userId);
  applicantsContainsMe = () => this.candidates().some(a => a.player.id === this.opts.userId);
  containsMe = () => this.opts.userId && (this.applicantsContainsMe() || this.pairingsContainMe());
  pairingsContainMe = () => this.data.pairings.some(a => a.player.id === this.opts.userId);

}
