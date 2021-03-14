import config from './config';
import CurrentPuzzle from 'puz/current';
import makePromotion from 'puz/promotion';
import throttle from 'common/throttle';
import { Api as CgApi } from 'chessground/api';
import { Clock } from 'puz/clock';
import { Combo } from 'puz/combo';
import { getNow, puzzlePov, sound } from 'puz/util';
import { makeCgOpts } from 'puz/run';
import { parseUci } from 'chessops/util';
import { Promotion, Run } from 'puz/interfaces';
import { prop, Prop } from 'common';
import { RacerOpts, RacerData, RacerVm, RacerPrefs, Race, UpdatableData, RaceStatus, WithGround } from './interfaces';
import { Role } from 'chessground/types';
import { Countdown } from './countdown';
import { Boost } from './boost';

export default class StormCtrl {
  private data: RacerData;
  private redraw: () => void;
  private sign = Math.random().toString(36);
  private localScore = 0;
  race: Race;
  pref: RacerPrefs;
  run: Run;
  vm: RacerVm;
  trans: Trans;
  promotion: Promotion;
  countdown: Countdown;
  boost: Boost = new Boost();
  ground = prop<CgApi | false>(false) as Prop<CgApi | false>;

  constructor(opts: RacerOpts, redraw: (data: RacerData) => void) {
    this.data = opts.data;
    this.race = this.data.race;
    this.pref = opts.pref;
    this.redraw = () => redraw(this.data);
    this.trans = lichess.trans(opts.i18n);
    this.run = {
      pov: puzzlePov(this.data.puzzles[0]),
      moves: 0,
      errors: 0,
      current: new CurrentPuzzle(0, this.data.puzzles[0]),
      clock: new Clock(config),
      history: [],
      combo: new Combo(config),
      modifier: {
        moveAt: 0,
      },
    };
    this.vm = {
      alreadyStarted: opts.data.startsIn && opts.data.startsIn <= 0,
    };
    this.countdown = new Countdown(this.run.clock, this.resetGround, this.redraw);
    this.promotion = makePromotion(this.withGround, this.cgOpts, this.redraw);
    this.boost.setPlayers(this.data.players);
    lichess.socket = new lichess.StrongSocket(`/racer/${this.race.id}`, false);
    lichess.socket.sign(this.sign);
    lichess.pubsub.on('socket.in.racerState', this.serverUpdate);
    setTimeout(() => {
      this.vm.startsAt = this.countdown.start(opts.data.startsIn, this.isPlayer());
      this.redraw();
    });
    // this.simulate();
    window.addEventListener('beforeunload', () => {
      if (this.isPlayer()) this.socketSend('racerEnd');
    });
  }

  serverUpdate = (data: UpdatableData) => {
    this.data.players = data.players;
    this.boost.setPlayers(data.players);
    this.vm.startsAt = this.countdown.start(data.startsIn, this.isPlayer()) || this.vm.startsAt;
    this.redraw();
    this.redrawSlow();
  };

  player = () => this.data.player;

  players = () => this.data.players;

  isPlayer = () => !this.vm.alreadyStarted && this.data.players.filter(p => p.name == this.data.player.name).length > 0;

  raceFull = () => this.data.players.length >= 10;

  status = (): RaceStatus =>
    this.vm.startsAt && this.vm.startsAt < new Date()
      ? this.data.players.some(p => !p.end)
        ? 'racing'
        : 'post'
      : 'pre';

  isRacing = () => this.status() == 'racing';

  myScore = (): number | undefined => {
    const p = this.data.players.find(p => p.name == this.data.player.name);
    return p?.score;
  };

  join = throttle(1000, () => {
    if (!this.isPlayer()) this.socketSend('racerJoin');
  });

  countdownSeconds = (): number | undefined =>
    this.vm.startsAt && this.vm.startsAt > new Date()
      ? Math.min(9, Math.ceil((this.vm.startsAt.getTime() - Date.now()) / 1000))
      : undefined;

  end = (): void => {
    this.run.history.reverse();
    this.run.endAt = getNow();
    this.resetGround();
    this.redraw();
    sound.end();
    this.redrawSlow();
    this.socketSend('racerEnd');
  };

  endNow = (): void => {
    this.pushToHistory(false);
    this.end();
  };

  userMove = (orig: Key, dest: Key): void => {
    if (!this.promotion.start(orig, dest, this.playUserMove)) this.playUserMove(orig, dest);
  };

  playUserMove = (orig: Key, dest: Key, promotion?: Role): void => {
    this.run.moves++;
    this.promotion.cancel();
    const puzzle = this.run.current;
    const uci = `${orig}${dest}${promotion ? (promotion == 'knight' ? 'n' : promotion[0]) : ''}`;
    const pos = puzzle.position();
    const move = parseUci(uci)!;
    let captureSound = pos.board.occupied.has(move.to);
    pos.play(move);
    if (pos.isCheckmate() || uci == puzzle.expectedMove()) {
      puzzle.moveIndex++;
      this.localScore++;
      this.run.combo.inc();
      this.run.modifier.moveAt = getNow();
      const bonus = this.run.combo.bonus();
      if (bonus) {
        this.run.modifier.bonus = bonus;
        this.localScore += bonus.seconds; // yeah, ah well
      }
      this.socketSend('racerScore', this.localScore);
      if (puzzle.isOver()) {
        this.pushToHistory(true);
        if (!this.incPuzzle()) this.end();
      } else {
        puzzle.moveIndex++;
        captureSound = captureSound || pos.board.occupied.has(parseUci(puzzle.line[puzzle.moveIndex]!)!.to);
      }
      sound.move(captureSound);
    } else {
      lichess.sound.play('error');
      this.pushToHistory(false);
      this.run.errors++;
      this.run.combo.reset();
      if (this.run.clock.flag()) this.end();
      else if (!this.incPuzzle()) this.end();
    }
    this.redraw();
    this.redrawQuick();
    this.redrawSlow();
    this.resetGround();
    lichess.pubsub.emit('ply', this.run.moves);
  };

  private redrawQuick = () => setTimeout(this.redraw, 100);
  private redrawSlow = () => setTimeout(this.redraw, 1000);

  private cgOpts = () =>
    this.isPlayer()
      ? makeCgOpts(this.run, this.isRacing() && !this.run.endAt)
      : {
          orientation: this.run.pov,
        };

  private resetGround = () => this.withGround(g => g.set(this.cgOpts()));

  private pushToHistory = (win: boolean) =>
    this.run.history.push({
      puzzle: this.run.current.puzzle,
      win,
      millis: this.run.history.length ? getNow() - this.run.current.startAt : 0, // first one is free
    });

  private incPuzzle = (): boolean => {
    const index = this.run.current.index;
    if (index < this.data.puzzles.length - 1) {
      this.run.current = new CurrentPuzzle(index + 1, this.data.puzzles[index + 1]);
      return true;
    }
    return false;
  };

  withGround: WithGround = f => {
    const g = this.ground();
    return g && f(g);
  };

  private socketSend = (tpe: string, data?: any) => lichess.socket.send(tpe, data, { sign: this.sign });

  private simulate = () => {
    this.data.players = [];
    for (let i = 0; i < 10; i++)
      this.data.players.push({
        name: `Player${i}`,
        score: 0,
      });
    setInterval(() => {
      if (true || this.isRacing()) {
        this.data.players[Math.floor(Math.random() * 10)].score++;
        this.boost.setPlayers(this.data.players);
        this.redraw();
        this.redrawSlow();
      }
    }, 150);
  };
}
