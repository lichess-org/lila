import { makeSocket, SwissSocket } from './socket';
import xhr from './xhr';
import throttle from 'common/throttle';
import { myPage, players } from './pagination';
import { SwissData, SwissOpts, Pages, Standing, Player } from './interfaces';

export default class SwissCtrl {
  data: SwissData;
  trans: Trans;
  socket: SwissSocket;
  page: number;
  pages: Pages = {};
  lastPageDisplayed: number | undefined;
  focusOnMe: boolean;
  joinSpinner = false;
  playerInfoId?: string;
  disableClicks = true;
  searching = false;

  private lastStorage = lichess.storage.make('last-redirect');

  constructor(readonly opts: SwissOpts, readonly redraw: () => void) {
    this.data = this.readData(opts.data);
    this.trans = lichess.trans(opts.i18n);
    this.socket = makeSocket(opts.socketSend, this);
    this.page = this.data.standing.page;
    this.focusOnMe = this.isIn();
    setTimeout(() => (this.disableClicks = false), 1500);
    this.loadPage(this.data.standing);
    this.scrollToMe();
    this.redirectToMyGame();
  }

  reload = (data: SwissData): void => {
    this.data = { ...this.data, ...this.readData(data) };
    this.data.me = data.me; // to account for removal on withdraw
    this.data.nextRound = data.nextRound; // to account for removal
    this.loadPage(this.data.standing);
    if (this.focusOnMe) this.scrollToMe();
    this.joinSpinner = false;
    this.redirectToMyGame();
    this.redrawNbRounds();
  };

  isCreated = () => this.data.status == 'created';
  isStarted = () => this.data.status == 'started';
  isFinished = () => this.data.status == 'finished';

  myGameId = () => this.data.me?.gameId;

  join = (password?: string) => {
    xhr.join(this, password);
    this.joinSpinner = true;
    this.focusOnMe = true;
  };

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

  scrollToMe = () => {
    const page = myPage(this);
    if (page && page !== this.page) this.setPage(page);
  };

  loadPage = (data: Standing) => {
    this.pages[data.page] = this.readStanding(data).players;
  };

  setPage = (page: number) => {
    this.page = page;
    xhr.loadPage(this, page);
  };

  toggleFocusOnMe = () => {
    if (this.data.me) {
      this.focusOnMe = !this.focusOnMe;
      if (this.focusOnMe) this.scrollToMe();
    }
  };

  toggleSearch = () => (this.searching = !this.searching);

  jumpToPageOf = (name: string) => {
    const userId = name.toLowerCase();
    xhr.loadPageOf(this, userId).then(data => {
      this.loadPage(data);
      this.page = data.page;
      this.searching = false;
      this.focusOnMe = false;
      this.pages[this.page].filter(p => p.user.id == userId).forEach(this.showPlayerInfo);
      this.redraw();
    });
  };

  userSetPage = (page: number) => {
    this.focusOnMe = false;
    this.setPage(page);
  };

  userNextPage = () => this.userSetPage(this.page + 1);
  userPrevPage = () => this.userSetPage(this.page - 1);
  userLastPage = () => this.userSetPage(players(this).nbPages);

  showPlayerInfo = (player: Player) => {
    this.playerInfoId = this.playerInfoId === player.user.id ? undefined : player.user.id;
    if (this.playerInfoId) xhr.playerInfo(this, this.playerInfoId);
  };

  askReload = () => {
    if (this.joinSpinner || (this.data.nextRound && this.data.me)) xhr.reloadNow(this);
    else this.reloadSoon();
  };

  withdraw = () => {
    xhr.withdraw(this);
    this.joinSpinner = true;
  };

  private reloadSoonThrottle: () => void;

  private reloadSoon = () => {
    if (!this.reloadSoonThrottle)
      this.reloadSoonThrottle = throttle(Math.max(2000, Math.min(5000, this.data.nbPlayers * 20)), () =>
        xhr.reloadNow(this)
      );
    this.reloadSoonThrottle();
  };

  private isIn = () => !!this.data.me && !this.data.me.absent;

  private redrawNbRounds = () => $('.swiss__meta__round').text(`${this.data.round}/${this.data.nbRounds}`);

  private readData = (data: SwissData) => ({
    ...data,
    standing: this.readStanding(data.standing),
  });

  private readStanding = (standing: Standing) => ({
    ...standing,
    players: standing.players.map(p => ({
      ...p,
      sheet: xhr.readSheetMin(p.sheetMin),
    })),
  });
}
