import makeSocket from './socket';
import xhr from './xhr';
import { myPage, players } from './pagination';
import * as sound from './sound';
import * as tour from './tournament';
import { TournamentData, TournamentOpts, Pages, PlayerInfo, TeamInfo, Standing } from './interfaces';
import { TournamentSocket } from './socket';

interface CtrlTeamInfo {
  requested?: string;
  loaded?: TeamInfo;
}

export default class TournamentController {

  opts: TournamentOpts;
  data: TournamentData;
  trans: Trans;
  socket: TournamentSocket;
  page: number;
  pages: Pages = {};
  lastPageDisplayed: number | undefined;
  focusOnMe: boolean;
  joinSpinner: boolean = false;
  playerInfo: PlayerInfo = {};
  teamInfo: CtrlTeamInfo = {};
  disableClicks: boolean = true;
  searching: boolean = false;
  joinWithTeamSelector: boolean = false;
  redraw: () => void;

  private watchingGameId: string;
  private lastStorage = window.lichess.storage.make('last-redirect');

  constructor(opts: TournamentOpts, redraw: () => void) {
    this.opts = opts;
    this.data = opts.data;
    this.redraw = redraw;
    this.trans = window.lichess.trans(opts.i18n);
    this.socket = makeSocket(opts.socketSend, this);
    this.page = this.data.standing.page;
    this.focusOnMe = tour.isIn(this);
    setTimeout(() => this.disableClicks = false, 1500);
    this.loadPage(this.data.standing);
    this.scrollToMe();
    sound.end(this.data);
    sound.countDown(this.data);
    this.redirectToMyGame();
    if (this.data.featured) this.startWatching(this.data.featured.id);
  }
