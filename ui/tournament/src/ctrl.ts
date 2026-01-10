import { type TournamentSocket, makeSocket } from './socket';
import * as xhr from './xhr';
import { maxPerPage, myPage, players } from './pagination';
import * as sound from './sound';
import type {
  TournamentData,
  TournamentOpts,
  Pages,
  PlayerInfo,
  TeamInfo,
  Standing,
  Player,
} from './interfaces';
import { storage, storedMapAsProp } from 'lib/storage';
import { pubsub } from 'lib/pubsub';
import { alerts, prompt } from 'lib/view';
import type { Prop } from 'lib';

interface CtrlTeamInfo {
  requested?: string;
  loaded?: TeamInfo;
}

export default class TournamentController {
  opts: TournamentOpts;
  data: TournamentData;
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
  nbWatchers = 0;
  collapsedDescription: Prop<boolean>;

  private lastStorage = storage.make('last-redirect');

  constructor(opts: TournamentOpts, redraw: () => void) {
    this.opts = opts;
    this.data = opts.data;
    this.redraw = redraw;
    this.socket = makeSocket(opts.socketSend, this);
    this.page = this.data.standing.page;
    this.focusOnMe = this.isIn();
    this.collapsedDescription = storedMapAsProp<boolean>(
      'tournament.collapsed-description-store',
      this.data.id,
      20,
      () => false,
    );
    setTimeout(() => (this.disableClicks = false), 1500);
    this.loadPage(this.data.standing);
    this.scrollToMe();
    sound.end(this.data);
    sound.countDown(this.data);
    this.setupBattle();
    this.redirectToMyGame();
    pubsub.on('socket.in.crowd', data => {
      this.nbWatchers = data.nb;
    });
  }

  askReload = (): void => {
    if (this.joinSpinner) xhr.reloadNow(this);
    else xhr.reloadSoon(this);
  };

  reload = (data: TournamentData): void => {
    const willChangeJoinStatus = !!this.data.me !== !!data.me || this.data.me?.withdraw !== data.me?.withdraw;
    // we joined a private tournament! Reload the page to load the chat
    if (!this.data.me && data.me && this.data.private) site.reload();
    this.data = { ...this.data, ...data, ...{ me: data.me } }; // to account for removal on withdraw
    if (data.playerInfo?.player.id === this.playerInfo.id) this.playerInfo.data = data.playerInfo!;
    this.loadPage(data.standing);
    if (this.focusOnMe) this.scrollToMe();
    sound.end(data);
    sound.countDown(data);
    if (willChangeJoinStatus) this.joinSpinner = false;
    this.recountTeams();
    this.redirectToMyGame();
  };

  myGameId = () => this.data.me?.gameId;

  private recountTeams() {
    if (this.data.teamBattle)
      this.data.teamBattle.hasMoreThanTenTeams = Object.keys(this.data.teamBattle.teams).length > 10;
  }

  private setupBattle() {
    if (this.data.teamBattle) {
      this.recountTeams();
      const locationTeam = location.hash.startsWith('#team/') && location.hash.slice(6);
      if (locationTeam) this.showTeamInfo(locationTeam);
    }
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
        site.redirect('/' + gameId, true);
      }
    }, delay);
  };

  loadPage = (data: Standing) => {
    if (!data.failed || !this.pages[data.page]) this.pages[data.page] = data.players;
  };

  setPage = (page: number | undefined) => {
    if (page && page !== this.page && page >= 1 && page <= players(this).nbPages) {
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
      this.pages[this.page].filter(p => p.name.toLowerCase() === userId).forEach(this.showPlayerInfo);
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

  join = async (team?: string) => {
    this.joinWithTeamSelector = false;
    if (!this.data.verdicts.accepted)
      return await alerts(this.data.verdicts.list.map(v => v.verdict).filter(v => v !== 'ok'));
    if (this.data.teamBattle && !team && !this.data.me) {
      this.joinWithTeamSelector = true;
    } else {
      let password;
      if (this.data.private && !this.data.me) {
        password = await prompt(i18n.site.tournamentEntryCode);
        if (password === null) {
          return;
        }
      }
      xhr.join(this, password, team);
      this.joinSpinner = true;
      this.focusOnMe = true;
    }
    return;
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

  isIn = () => !!this.data.me && !this.data.me.withdraw;

  willBePaired = () => this.isIn() && !this.data.pairingsClosed;
}
