import {
  type StoredBooleanProp,
  type StoredJsonProp,
  storedJsonProp,
  storedProp,
} from 'common/storage';
import type {
  Arrangement,
  NewArrangement,
  NewArrangementSettings,
  Pages,
  PlayerInfo,
  Standing,
  TeamInfo,
  TournamentData,
  TournamentOpts,
} from './interfaces';
import { maxPerPage, myPage, players } from './pagination';
import makeSocket, { type TournamentSocket } from './socket';
import * as sound from './sound';
import * as tour from './tournament';
import xhr from './xhr';

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
  playerInfo: PlayerInfo = {};
  teamInfo: CtrlTeamInfo = {};
  arrangement: Arrangement | undefined;
  utc: StoredBooleanProp = storedProp('arrangement.utc', false);
  playerManagement = false;
  newArrangement: NewArrangement | undefined;
  newArrangementSettings: StoredJsonProp<NewArrangementSettings>;
  shadedCandidates: string[] = [];
  highlightArrs: string[] = [];
  disableClicks = true;
  searching = false;
  joinWithTeamSelector = false;
  redraw: () => void;
  nbWatchers = 0;

  private lastStorage = window.lishogi.storage.make('last-redirect');

  constructor(opts: TournamentOpts, redraw: () => void) {
    this.opts = opts;
    this.data = opts.data;
    this.redraw = redraw;
    this.socket = makeSocket(opts.socketSend, this);
    this.page = this.data.standing.page || 1;
    this.focusOnMe = tour.isIn(this);
    setTimeout(() => (this.disableClicks = false), 1500);
    this.loadPage(this.data.standing);
    this.scrollToMe();
    sound.end(this.data);
    sound.countDown(this.data);
    this.recountTeams();
    this.redirectToMyGame();

    this.newArrangementSettings = storedJsonProp(
      `arrangement.newArrangementSettings${this.data.id}`,
      () => {
        return { points: { w: 3, d: 2, l: 1 } };
      },
    );
    console.log('Data', this.data);
    // this.newArrangement = this.newArrangementSettings();

    const hash = window.location.hash,
      userIds = hash.slice(1).split(';');
    if (
      userIds.length === 2 &&
      userIds[0] !== userIds[1] &&
      this.data.system !== 'arena' &&
      userIds.every(u => this.data.standing.players.some(p => p.id === u))
    )
      this.arrangement = this.findOrCreateArrangement(userIds);
    else if (hash.length === 8)
      this.arrangement = this.data.standing.arrangements.find(a => a.id === hash);

    window.lishogi.pubsub.on('socket.in.crowd', data => {
      this.nbWatchers = data.nb;
    });
  }

  askReload = (): void => {
    if (this.joinSpinner) xhr.reloadNow(this);
    else xhr.reloadSoon(this);
  };

  reload = (data: TournamentData): void => {
    // we joined a private tournament! Reload the page to load the chat
    if (!this.data.me && data.me && this.data.private) window.lishogi.reload();

    this.data = { ...this.data, ...data };
    this.data.me = data.me;
    this.data.isCandidate = data.isCandidate;
    this.data.isDenied = data.isDenied;
    this.data.isClosed = data.isClosed;
    this.data.candidatesOnly = data.candidatesOnly;
    this.data.candidatesFull = data.candidatesFull;
    if (data.playerInfo && data.playerInfo.player.id === this.playerInfo.id)
      this.playerInfo.data = data.playerInfo;

    this.loadPage(data.standing);
    if (this.focusOnMe) this.scrollToMe();
    sound.end(data);
    sound.countDown(data);
    this.shadedCandidates = [];
    this.joinSpinner = false;
    this.recountTeams();
    this.redirectToMyGame();
  };

  isRobin = (): boolean => this.data.system === 'robin';
  isOrganized = (): boolean => this.data.system === 'organized';
  isArena = (): boolean => this.data.system === 'arena';

  isCreator = (): boolean => this.data.createdBy === this.opts.userId;

  myGameId = (): string => {
    return this.data.me?.gameId;
  };

  private recountTeams() {
    if (this.data.teamBattle)
      this.data.teamBattle.hasMoreThanTenTeams =
        Object.keys(this.data.teamBattle.teams).length > 10;
  }

  private redirectToMyGame() {
    const gameId = this.myGameId();
    if (gameId) this.redirectFirst(gameId);
  }

  redirectFirst = (gameId: string, rightNow?: boolean): void => {
    const delay = rightNow || document.hasFocus() ? 10 : 1000 + Math.random() * 500;
    setTimeout(() => {
      if (this.lastStorage.get() !== gameId) {
        this.lastStorage.set(gameId);
        window.lishogi.redirect(`/${gameId}`);
      }
    }, delay);
  };

  loadPage = (data: Standing): void => {
    if (this.isArena()) {
      if (!data.failed || !this.pages[data.page]) this.pages[data.page] = data.players;
    } else if (this.isOrganized()) {
      console.log('p', data.players);
      for (let i = 1; i <= Math.ceil(data.players.length / maxPerPage); i++)
        this.pages[i] = data.players.slice((i - 1) * maxPerPage, i * maxPerPage);
    }
    console.log('PAGES', this.pages);
  };

  setPage = (page: number): void => {
    this.page = page;
    if (this.isArena()) xhr.loadPage(this, page);
  };

  jumpToPageOf = (name: string): void => {
    const userId = name.toLowerCase();
    if (this.isArena()) {
      xhr.loadPageOf(this, userId).then(data => {
        this.loadPage(data);
        this.page = data.page;
        this.searching = false;
        this.focusOnMe = false;
        this.pages[this.page]
          .filter(p => p.name.toLowerCase() == userId)
          .forEach(this.showPlayerInfo);
      });
    } else {
      const index = this.data.standing.players.findIndex(p => p.id === userId);
      if (index !== -1) this.page = Math.floor(index / maxPerPage) + 1;
      this.searching = false;
      this.focusOnMe = false;
    }
    this.redraw();
  };

  userSetPage = (page: number): void => {
    this.focusOnMe = false;
    this.setPage(page);
  };

  userNextPage = (): void => this.userSetPage(this.page + 1);
  userPrevPage = (): void => this.userSetPage(this.page - 1);
  userLastPage = (): void => this.userSetPage(players(this).nbPages);

  withdraw = (): void => {
    xhr.withdraw(this);
    this.joinSpinner = true;
    this.focusOnMe = false;
  };

  join = (password?: string, team?: string): any => {
    this.joinWithTeamSelector = false;
    if (!this.data.verdicts.accepted)
      return this.data.verdicts.list.forEach(v => {
        if (v.verdict !== 'ok') alert(v.verdict);
      });
    if (this.data.teamBattle && !team && !this.data.me) {
      this.joinWithTeamSelector = true;
    } else {
      xhr.join(this, password, team);
      this.joinSpinner = true;
      this.focusOnMe = true;
    }
  };

  processCandidate(userId: string, accept: boolean): void {
    this.shadedCandidates.push(userId);
    this.socket.send('process-candidate', { u: userId, v: accept });
    this.redraw();
  }

  playerKick(userId: string): void {
    this.socket.send('player-kick', { v: userId });
    this.redraw();
  }

  scrollToMe = (): void => {
    if (this.isArena() || this.isOrganized()) {
      const page = myPage(this);
      if (page && page !== this.page) this.setPage(page);
    }
  };

  toggleFocusOnMe = (): void => {
    if (!this.data.me) return;
    this.focusOnMe = !this.focusOnMe;
    if (this.focusOnMe) this.scrollToMe();
  };

  findArrangement = (users: string[]): Arrangement | undefined => {
    return this.data.standing.arrangements.find(
      a => users.includes(a.user1.id) && users.includes(a.user2.id),
    );
  };

  findOrCreateArrangement = (users: string[]): Arrangement => {
    const existing = this.findArrangement(users);
    if (existing) return existing;
    else {
      const arr = {
        id: undefined,
        user1: {
          id: users[0],
        },
        user2: {
          id: users[1],
        },
      };
      // this.data.standing.arrangements.push(arr);
      return arr;
    }
  };

  showArrangement = (arrangement: Arrangement | undefined): void => {
    console.log('showArrangement', arrangement);
    this.arrangement = arrangement;
    if (arrangement)
      window.history.replaceState(null, '', `#${arrangement.user1.id};${arrangement.user2.id}`);
    else history.replaceState(null, '', window.location.pathname + window.location.search);
    this.redraw();
    window.scrollTo({
      top: 0,
      behavior: 'smooth',
    });
  };

  arrangementMatch = (arrangement: Arrangement, yes: boolean): void => {
    console.log('arrangementMatch', arrangement, yes);

    this.socket.send('arrangement-match', {
      id: arrangement.id,
      users: `${arrangement.user1.id};${arrangement.user2.id}`,
      y: yes,
    });
  };

  arrangementTime = (arrangement: Arrangement, date: Date | undefined): void => {
    console.log('arrangementTime', arrangement, date, date?.getTime());
    const data = {
      id: arrangement.id,
      users: `${arrangement.user1.id};${arrangement.user2.id}`,
    };
    if (date) data.t = date.getTime();
    this.socket.send('arrangement-time', data);
  };

  showOrganizerArrangement(): void {
    this.newArrangement = this.newArrangementSettings();
  }

  showPlayerInfo = (player: any): void => {
    if (this.data.secondsToStart) return;
    const userId = player.name.toLowerCase();
    this.teamInfo.requested = undefined;
    this.playerInfo = {
      id: this.playerInfo.id === userId ? null : userId,
      player: player,
      data: null,
    };
    if (this.playerInfo.id) xhr.playerInfo(this, this.playerInfo.id);
  };

  setPlayerInfoData = (data: PlayerInfo): void => {
    if (data.player.id === this.playerInfo.id) this.playerInfo.data = data;
  };

  showTeamInfo = (teamId: string): void => {
    this.playerInfo.id = undefined;
    this.teamInfo = {
      requested: this.teamInfo.requested === teamId ? undefined : teamId,
      loaded: undefined,
    };
    if (this.teamInfo.requested) xhr.teamInfo(this, this.teamInfo.requested);
  };

  setTeamInfo = (teamInfo: TeamInfo): void => {
    if (teamInfo.id === this.teamInfo.requested) this.teamInfo.loaded = teamInfo;
  };

  toggleSearch = (): void => {
    this.searching = !this.searching;
  };
}
