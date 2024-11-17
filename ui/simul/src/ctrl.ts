import { makeSocket, type SimulSocket } from './socket';
import xhr from './xhr';
import type { SimulData, SimulOpts } from './interfaces';
import { storage } from 'common/storage';
import { idleTimer } from 'common/timing';

export default class SimulCtrl {
  data: SimulData;
  socket: SimulSocket;

  constructor(
    readonly opts: SimulOpts,
    readonly redraw: () => void,
  ) {
    this.data = opts.data;
    this.socket = makeSocket(opts.socketSend, this);
    if (this.createdByMe() && this.data.isCreated) this.setupCreatedHost();
  }

  private setupCreatedHost = () => {
    storage.set('site.move_on', '1'); // hideous hack :D
    let hostIsAround = true;
    idleTimer(
      15 * 60 * 1000,
      () => {
        hostIsAround = false;
      },
      () => {
        hostIsAround = true;
      },
    );
    setInterval(() => {
      if (this.data.isCreated && hostIsAround) xhr.ping(this.data.id);
    }, 10 * 1000);
  };

  reload = (data: SimulData) => {
    this.data = data;
  };

  createdByMe = () => this.opts.userId === this.data.host.id;
  candidates = () => this.data.applicants.filter(a => !a.accepted);
  accepted = () => this.data.applicants.filter(a => a.accepted);
  acceptedContainsMe = () => this.accepted().some(a => a.player.id === this.opts.userId);
  applicantsContainsMe = () => this.candidates().some(a => a.player.id === this.opts.userId);
  containsMe = () =>
    this.opts.userId &&
    (this.applicantsContainsMe() || this.acceptedContainsMe() || this.pairingsContainMe());
  pairingsContainMe = () => this.data.pairings.some(a => a.player.id === this.opts.userId);
}
