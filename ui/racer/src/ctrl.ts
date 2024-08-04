import config from './config';
import CurrentPuzzle from 'puz/current';
import throttle, { throttlePromiseDelay } from 'common/throttle';
import * as xhr from 'common/xhr';
import { Api as CgApi } from 'chessground/api';
import { Boost } from './boost';
import { Clock } from 'puz/clock';
import { Combo } from 'puz/combo';
import { Countdown } from './countdown';
import { getNow, puzzlePov, sound } from 'puz/util';
import { makeCgOpts } from 'puz/run';
import { parseUci } from 'chessops/util';
import { PuzCtrl, Run } from 'puz/interfaces';
import { PuzFilters } from 'puz/filters';
import { defined, prop } from 'common';
import {
  RacerOpts,
  RacerData,
  RacerVm,
  RacerPrefs,
  Race,
  UpdatableData,
  RaceStatus,
  WithGround,
} from './interfaces';
import { Role } from 'chessground/types';
import { storedBooleanProp } from 'common/storage';
import { PromotionCtrl } from 'chess/promotion';

export default class RacerCtrl implements PuzCtrl {
  private data: RacerData;
  private sign = Math.random().toString(36);
  private localScore = 0;
  race: Race;
  pref: RacerPrefs;
  run: Run;
  vm: RacerVm;
  filters: PuzFilters;
  trans: Trans;
  promotion: PromotionCtrl;
  countdown: Countdown;
  boost: Boost = new Boost();
  skipAvailable = true;
  knowsSkip = storedBooleanProp('racer.skip', false);
  ground = prop<CgApi | false>(false);
  flipped = false;
  redrawInterval: Timeout;

  constructor(
    opts: RacerOpts,
    readonly redraw: () => void,
  ) {
    this.data = opts.data;
    this.race = this.data.race;
    this.pref = opts.pref;
    this.filters = new PuzFilters(redraw, true);
    this.trans = site.trans(opts.i18n);
    this.run = {
      pov: puzzlePov(this.data.puzzles[0]),
      moves: 0,
      errors: 0,
      current: new CurrentPuzzle(0, this.data.puzzles[0]),
      clock: new Clock(config, defined(opts.data.startsIn) ? Math.max(0, -opts.data.startsIn) : undefined),
      history: [],
      combo: new Combo(config),
      modifier: {
        moveAt: 0,
      },
    };
    this.vm = {
      alreadyStarted: defined(opts.data.startsIn) && opts.data.startsIn <= 0,
    };
    this.countdown = new Countdown(
      this.run.clock,
      () => {
        this.setGround();
        this.run.current.moveIndex = 0;
        this.setGround();
      },
      () => setTimeout(this.redraw),
    );
    this.promotion = new PromotionCtrl(this.withGround, this.setGround, this.redraw);
    this.serverUpdate(opts.data);
    site.socket = new site.StrongSocket(`/racer/${this.race.id}`, false, {
      events: {
        racerState: (data: UpdatableData) => {
          this.serverUpdate(data);
          this.redraw();
          this.redrawSlow();
        },
      },
    });
    site.socket.sign(this.sign);
    site.pubsub.on('zen', () => {
      const zen = $('body').toggleClass('zen').hasClass('zen');
      window.dispatchEvent(new Event('resize'));
      this.setZen(zen);
    });
    $('#zentog').on('click', this.toggleZen);
    this.redrawInterval = setInterval(this.redraw, 1000);
    setTimeout(this.hotkeys, 1000);
  }

  serverUpdate = (data: UpdatableData) => {
    this.data.players = data.players;
    this.boost.setPlayers(data.players);
    if (data.startsIn && this.status() == 'pre') {
      this.vm.startsAt = new Date(Date.now() + data.startsIn);
      this.run.current.startAt = getNow() + data.startsIn;
      if (data.startsIn > 0) this.countdown.start(this.vm.startsAt, this.isPlayer());
      else this.run.clock.start();
    }
  };

  player = () => this.data.player;

  players = () => this.data.players;

  isPlayer = () => !this.vm.alreadyStarted && this.data.players.some(p => p.name == this.data.player.name);

  raceFull = () => this.data.players.length >= 10;

  status = (): RaceStatus => (this.run.clock.started() ? (this.run.clock.flag() ? 'post' : 'racing') : 'pre');

  isRacing = () => this.status() == 'racing';

  isOwner = () => this.data.owner;

  myScore = (): number | undefined => {
    const p = this.data.players.find(p => p.name == this.data.player.name);
    return p?.score;
  };

  join = throttle(1000, () => {
    if (!this.isPlayer()) this.socketSend('racerJoin');
  });

  start = throttle(1000, () => {
    if (this.isOwner()) this.socketSend('racerStart');
  });

  countdownSeconds = (): number | undefined =>
    this.status() == 'pre' && this.vm.startsAt && this.vm.startsAt > new Date()
      ? Math.min(10, Math.ceil((this.vm.startsAt.getTime() - Date.now()) / 1000))
      : undefined;

  end = (): void => {
    this.pushToHistory(false); // add last unsolved puzzle
    this.setGround();
    this.redraw();
    sound.end();
    site.pubsub.emit('ply', 0); // restore resize handle
    $('body').toggleClass('playing'); // end zen
    this.redrawSlow();
    clearInterval(this.redrawInterval);
  };

  canSkip = () => this.skipAvailable;

  skip = () => {
    if (this.skipAvailable && this.run.clock.started()) {
      this.skipAvailable = false;
      sound.good();
      this.run.skipId = this.run.current.puzzle.id;
      this.playUci(this.run.current.expectedMove());
      this.knowsSkip(true);
    }
  };

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, { submit: this.playUserMove })) this.playUserMove(orig, dest);
  };

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void =>
    this.playUci(`${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`);

  playUci = (uci: Uci): void => {
    const now = getNow();
    const puzzle = this.run.current;
    if (puzzle.startAt + config.minFirstMoveTime > now) console.log('reverted!');
    else {
      this.run.moves++;
      this.promotion.cancel();
      const pos = puzzle.position();
      pos.play(parseUci(uci)!);
      if (pos.isCheckmate() || uci == puzzle.expectedMove()) {
        puzzle.moveIndex++;
        this.localScore++;
        this.run.combo.inc();
        this.run.modifier.moveAt = now;
        const bonus = this.run.combo.bonus();
        if (bonus) {
          this.run.modifier.bonus = bonus;
          this.localScore += bonus.seconds; // yeah, ah well
        }
        this.socketSend('racerScore', this.localScore);
        if (puzzle.isOver()) {
          if (!this.incPuzzle(true)) this.end();
        } else {
          puzzle.moveIndex++;
        }
        this.run.current.playSound(puzzle);
      } else {
        sound.wrong();
        this.run.errors++;
        this.run.combo.reset();
        if (this.run.clock.flag()) this.end();
        else if (!this.incPuzzle(false)) this.end();
      }
      this.redraw();
      this.redrawQuick();
      this.redrawSlow();
    }
    this.setGround();
    if (this.run.current.moveIndex < 0) {
      this.run.current.moveIndex = 0;
      this.setGround();
    }
    site.pubsub.emit('ply', this.run.moves);
  };

  private redrawQuick = () => setTimeout(this.redraw, 100);
  private redrawSlow = () => setTimeout(this.redraw, 1000);

  private cgOpts = () =>
    this.isPlayer()
      ? makeCgOpts(this.run, this.isRacing(), this.flipped)
      : {
          orientation: this.run.pov,
        };

  private setGround = () => this.withGround(g => g.set(this.cgOpts()));

  private incPuzzle = (win: boolean): boolean => {
    this.pushToHistory(win);
    const index = this.run.current.index;
    if (index < this.data.puzzles.length - 1) {
      this.run.current = new CurrentPuzzle(index + 1, this.data.puzzles[index + 1]);
      return true;
    }
    return false;
  };

  private pushToHistory = (win: boolean) =>
    this.run.history.push({
      puzzle: this.data.puzzles[this.run.current.index],
      win,
      millis: getNow() - this.run.current.startAt,
    });

  withGround: WithGround = f => {
    const g = this.ground();
    return g && f(g);
  };

  flip = () => {
    this.flipped = !this.flipped;
    this.withGround(g => g.toggleOrientation());
    this.redraw();
  };

  private socketSend = (tpe: string, data?: any) => site.socket.send(tpe, data, { sign: this.sign });

  private setZen = throttlePromiseDelay(
    () => 1000,
    zen =>
      xhr.text('/pref/zen', {
        method: 'post',
        body: xhr.form({ zen: zen ? 1 : 0 }),
      }),
  );

  private toggleZen = () => site.pubsub.emit('zen');

  private hotkeys = () => site.mousetrap.bind('f', this.flip).bind('z', this.toggleZen);
}
