import makeSocket from './socket';
import * as xhr from './xhr';
import { maxPerPage, myPage, players } from './pagination';
import * as sound from './sound';
import * as tour from './tournament';
import { TournamentData, TournamentOpts, Pages, PlayerInfo, TeamInfo, Standing, Player } from './interfaces';
// eslint-disable-next-line no-duplicate-imports
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
  joinSpinner = false;
  playerInfo: { id?: string; player?: Player; data?: PlayerInfo } = {};
  teamInfo: CtrlTeamInfo = {};
  disableClicks = true;
  searching = false;
  joinWithTeamSelector = false;
  redraw: () => void;

  private lastStorage = lichess.storage.make('last-redirect');

  constructor(opts: TournamentOpts, redraw: () => void) {
    this.opts = opts;
    this.data = opts.data;
    this.redraw = redraw;
    this.trans = lichess.trans(opts.i18n);
    this.socket = makeSocket(opts.socketSend, this);
    this.page = this.data.standing.page;
    this.focusOnMe = tour.isIn(this);
    setTimeout(() => (this.disableClicks = false), 1500);
    this.loadPage(this.data.standing);
    this.scrollToMe();
    sound.end(this.data);
    sound.countDown(this.data);
    this.recountTeams();
    this.redirectToMyGame();
  }

  askReload = (): void => {
    if (this.joinSpinner) xhr.reloadNow(this);
    else xhr.reloadSoon(this);
  };

  reload = (data: TournamentData): void => {
    // we joined a private tournament! Reload the page to load the chat
    if (!this.data.me && data.me && this.data.private) lichess.reload();
    this.data = { ...this.data, ...data };
    this.data.me = data.me; // to account for removal on withdraw
    if (data.playerInfo?.player.id === this.playerInfo.id) this.playerInfo.data = data.playerInfo!;
    this.loadPage(data.standing);
    if (this.focusOnMe) this.scrollToMe();
    sound.end(data);
    sound.countDown(data);
    this.joinSpinner = false;
    this.recountTeams();
    this.redirectToMyGame();
  };

  myGameId = () => this.data.me?.gameId;

  private recountTeams() {
    if (this.data.teamBattle)
      this.data.teamBattle.hasMoreThanTenTeams = Object.keys(this.data.teamBattle.teams).length > 10;
  }

  private redirectToMyGame() {
    const gameId = this.myGameId();
    if (gameId) this.redirectFirst(gameId);
  }

  redirectFirst = (gameId: string, rightNow?: boolean) => {
    const delay = rightNow || document.hasFocus() ? 10 : 1000 + Math.random() * 500;
    setTimeout(() => {
      if (this.lastStorage.get() !== gameId) {
        this.lastStorage.set(gameId);
        lichess.redirect('/' + gameId);
      }
    }, delay);
  };

  loadPage = (data: Standing) => {
    if (!data.failed || !this.pages[data.page]) this.pages[data.page] = data.players;
  };

  setPage = (page: number | undefined) => {
    if (page && page != this.page && page >= 1 && page <= players(this).nbPages) {
      this.page = page;
      xhr.loadPage(this, page);
    }
  };

  jumpToPageOf = (name: string) => {
    const userId = name.toLowerCase();
    xhr.loadPageOf(this, userId).then(data => {
      this.loadPage(data);
      this.page = data.page;
      this.searching = false;
      this.focusOnMe = false;
      this.pages[this.page].filter(p => p.name.toLowerCase() == userId).forEach(this.showPlayerInfo);
      this.redraw();
    });
  };

  jumpToRank = (rank: number) => {
    const page = 1 + Math.floor((rank - 1) / maxPerPage);
    const row = (rank - 1) % maxPerPage;
    xhr.loadPage(this, page, () => {
      if (!this.pages[page] || row >= this.pages[page].length) return;
      this.page = page;
      this.searching = false;
      this.focusOnMe = false;
      this.showPlayerInfo(this.pages[page][row]);
    });
  };

  userSetPage = (page: number) => {
    this.focusOnMe = false;
    this.setPage(page);
  };

  userNextPage = () => this.userSetPage(this.page + 1);
  userPrevPage = () => this.userSetPage(this.page - 1);
  userLastPage = () => this.userSetPage(players(this).nbPages);

  withdraw = () => {
    xhr.withdraw(this);
    this.joinSpinner = true;
    this.focusOnMe = false;
  };

  join = (team?: string) => {
    this.joinWithTeamSelector = false;
    if (!this.data.verdicts.accepted)
      return this.data.verdicts.list.forEach(v => {
        if (v.verdict !== 'ok') alert(v.verdict);
      });
    if (this.data.teamBattle && !team && !this.data.me) {
      this.joinWithTeamSelector = true;
    } else {
      let password;
      if (this.data.private && !this.data.me) {
        password = prompt(this.trans.noarg('tournamentEntryCode'));
        if (password === null) {
          return;
        }
      }
      xhr.join(this, password, team);
      this.joinSpinner = true;
      this.focusOnMe = true;
    }
  };

  scrollToMe = () => this.setPage(myPage(this));

  toggleFocusOnMe = () => {
    if (!this.data.me) return;
    this.focusOnMe = !this.focusOnMe;
    if (this.focusOnMe) this.scrollToMe();
  };

  showPlayerInfo = (player: Player) => {
    if (this.data.secondsToStart) return;
    const userId = player.name.toLowerCase();
    this.teamInfo.requested = undefined;
    this.playerInfo = {
      id: this.playerInfo.id === userId ? undefined : userId,
      player: player,
    };
    if (this.playerInfo.id) xhr.playerInfo(this, this.playerInfo.id);
  };

  setPlayerInfoData = (data: PlayerInfo) => {
    if (data.player.id === this.playerInfo.id) this.playerInfo.data = data;
  };

  showTeamInfo = (teamId: string) => {
    this.playerInfo.id = undefined;
    this.teamInfo = {
      requested: this.teamInfo.requested === teamId ? undefined : teamId,
      loaded: undefined,
    };
    if (this.teamInfo.requested) xhr.teamInfo(this, this.teamInfo.requested);
  };

  setTeamInfo = (teamInfo: TeamInfo) => {
    if (teamInfo.id === this.teamInfo.requested) this.teamInfo.loaded = teamInfo;
  };

  toggleSearch = () => {
    this.searching = !this.searching;
  };
}
