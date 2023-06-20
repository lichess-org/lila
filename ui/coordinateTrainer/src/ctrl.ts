import { sparkline } from '@fnando/sparkline';
import * as xhr from 'common/xhr';
import { throttlePromiseDelay } from 'common/throttle';
import { withEffect } from 'common';
import { makeCtrl as makeVoiceCtrl, VoiceCtrl } from 'voice';
import { storedBooleanProp, storedProp } from 'common/storage';
import { Api as CgApi } from 'chessground/api';
import {
  ColorChoice,
  TimeControl,
  CoordinateTrainerConfig,
  InputMethod,
  Mode,
  ModeScores,
  Redraw,
} from './interfaces';

const orientationFromColorChoice = (colorChoice: ColorChoice): Color =>
  (colorChoice === 'random' ? ['white', 'black'][Math.round(Math.random())] : colorChoice) as Color;

const randomChoice = (max: number) => Math.floor(Math.random() * max);

const newKey = (oldKey: Key | '', selectedFiles?: Set<Files>, selectedRanks?: Set<Ranks>): Key => {
  const rand = randomChoice(2);
  let files = 'abcdefgh'.split('') as Files[];
  let rows = '12345678'.split('') as Ranks[];

  if (selectedFiles?.size) {
    files = files.filter((f: Files) => selectedFiles.has(f));
  }
  if (selectedRanks?.size) rows = rows.filter((r: Ranks) => selectedRanks.has(r));

  // disallow the previous coordinate's row or file from being selected
  // rand so we only change one of them
  if (files.length > 1 && rand === 0) files = files.filter(f => f !== oldKey[0]);
  if (rows.length > 1 && rand === 1) rows = rows.filter(r => r !== oldKey[1]);

  return (files[randomChoice(files.length)] + rows[randomChoice(rows.length)]) as Key;
};

const targetSvg = (target: 'current' | 'next'): string => `
<g transform="translate(50, 50)">
  <rect class="${target}-target" fill="none" stroke-width="10" x="-50" y="-50" width="100" height="100" rx="5" />
</g>
`;

const rankWords: { [_: string]: string } = {
  one: '1',
  two: '2',
  three: '3',
  four: '4',
  five: '5',
  six: '6',
  seven: '7',
  eight: '8',
};

export const DURATION = 30 * 1000;
const TICK_DELAY = 50;

export default class CoordinateTrainerCtrl {
  chessground: CgApi | undefined;
  currentKey: Key | '' = 'a1';
  hasPlayed = false;
  isAuth = document.body.hasAttribute('data-user');
  keyboardInput: HTMLInputElement;
  voice: VoiceCtrl;
  modeScores: ModeScores = this.config.scores;
  nextKey: Key | '' = newKey('a1');
  playing = false;
  score = 0;
  timeAtStart: Date;
  timeLeft = DURATION;
  trans: Trans = lichess.trans(this.config.i18n);
  wrong: boolean;
  wrongTimeout: number;
  zen: boolean;

  constructor(readonly config: CoordinateTrainerConfig, readonly redraw: Redraw) {
    const setZen = throttlePromiseDelay(
      () => 1000,
      zen =>
        xhr.text('/pref/zen', {
          method: 'post',
          body: xhr.form({ zen: zen ? 1 : 0 }),
        })
    );

    lichess.pubsub.on('zen', () => {
      const zen = $('body').toggleClass('zen').hasClass('zen');
      window.dispatchEvent(new Event('resize'));
      setZen(zen);
    });

    $('#zentog').on('click', () => lichess.pubsub.emit('zen'));
    window.Mousetrap.bind('z', () => lichess.pubsub.emit('zen'));

    window.Mousetrap.bind('enter', () => (this.playing ? null : this.start()));

    window.addEventListener('resize', () => requestAnimationFrame(this.updateCharts), true);

    this.voice = makeVoiceCtrl({ redraw: this.redraw, tpe: 'coords' });
    lichess.mic.initRecognizer([...'abcdefgh', ...Object.keys(rankWords), 'start', 'stop'], {
      partial: true,
      listener: this.onVoice.bind(this),
    });
  }

  colorChoice = withEffect<ColorChoice>(
    storedProp<ColorChoice>(
      'coordinateTrainer.colorChoice',
      'random',
      str => str as ColorChoice,
      v => v
    ),
    () => this.setOrientationFromColorChoice()
  );

  orientation = orientationFromColorChoice(this.colorChoice());

  setOrientationFromColorChoice = () => {
    this.orientation = orientationFromColorChoice(this.colorChoice());
    if (this.chessground!.state.orientation !== this.orientation) this.chessground!.toggleOrientation();
    this.redraw();
  };

  mode = withEffect<Mode>(
    storedProp<Mode>(
      'coordinateTrainer.mode',
      window.location.hash === '#name' ? 'nameSquare' : 'findSquare',
      str => str as Mode,
      v => v
    ),
    () => this.onModeChange()
  );

  onModeChange = () => {
    this.redraw();
    this.updateCharts();
    window.location.hash = `#${this.mode().substring(0, 4)}`;
  };

  selectionEnabled = withEffect<boolean>(
    storedBooleanProp('coordinateTrainer.selectionEnabled', false),
    this.redraw
  );

  selectedFiles = new Set<Files>();
  selectedRanks = new Set<Ranks>();

  onFilesChange = (file: Files, on: boolean) => {
    if (on) this.selectedFiles.add(file);
    else this.selectedFiles.delete(file);
  };

  onRanksChange = (rank: Ranks, on: boolean) => {
    if (on) this.selectedRanks.add(rank);
    else this.selectedRanks.delete(rank);
  };

  timeControl = withEffect(
    storedProp<TimeControl>(
      'coordinateTrainer.timeControl',
      document.body.classList.contains('kid') ? 'untimed' : 'thirtySeconds',
      str => str as TimeControl,
      v => v
    ),
    this.redraw
  );

  timeDisabled = () => this.timeControl() === 'untimed';

  showCoordinates = withEffect<boolean>(
    storedBooleanProp('coordinateTrainer.showCoordinates', document.body.classList.contains('kid')),
    (show: boolean) => this.onShowCoordinatesChange(show)
  );

  onShowCoordinatesChange = (show: boolean) => {
    this.chessground?.set({ coordinates: show });
    this.chessground?.redrawAll();
  };

  showPieces = withEffect<boolean>(storedBooleanProp('coordinateTrainer.showPieces', true), () =>
    this.onShowPiecesChange()
  );

  onShowPiecesChange = () => {
    this.chessground?.set({ fen: this.boardFEN() });
    this.chessground?.redrawAll();
  };

  boardFEN = () => (this.showPieces() ? 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR' : '8/8/8/8/8/8/8/8');

  coordinateInputMethod = withEffect(
    storedProp<InputMethod>(
      'coordinateTrainer.coordinateInputMethod',
      window.innerWidth >= 980 ? 'text' : 'buttons',
      str => str as InputMethod,
      v => v
    ),
    this.redraw
  );

  toggleInputMethod = () =>
    this.coordinateInputMethod(this.coordinateInputMethod() === 'text' ? 'buttons' : 'text');

  start = () => {
    if (this.playing) return;
    this.playing = true;
    this.hasPlayed = true;
    this.score = 0;
    this.timeLeft = DURATION;
    this.currentKey = '';
    this.nextKey = '';

    // Redraw the chessground to remove the resize handle
    this.chessground?.redrawAll();

    // In case random is selected, recompute orientation
    this.setOrientationFromColorChoice();

    if (this.mode() === 'nameSquare') this.keyboardInput?.focus();

    setTimeout(() => {
      // Advance coordinates twice in order to get an entirely new set
      this.advanceCoordinates();
      this.advanceCoordinates();

      this.timeAtStart = new Date();
      if (!this.timeDisabled()) this.tick();
    }, 1000);
  };

  private tick = () => {
    if (!this.playing) return;
    const timeSpent = Math.min(DURATION, new Date().getTime() - +this.timeAtStart);
    this.timeLeft = DURATION - timeSpent;
    this.redraw();

    if (this.timeLeft > 0) setTimeout(this.tick, TICK_DELAY);
    else this.stop();
  };

  advanceCoordinates = () => {
    this.currentKey = this.nextKey;
    if (this.selectionEnabled() === true)
      this.nextKey = newKey(this.nextKey, this.selectedFiles, this.selectedRanks);
    else this.nextKey = newKey(this.nextKey);

    if (this.mode() === 'nameSquare')
      this.chessground?.setShapes([
        { orig: this.currentKey as Key, customSvg: targetSvg('current'), brush: '' },
        { orig: this.nextKey as Key, customSvg: targetSvg('next'), brush: '' },
      ]);

    this.redraw();
  };

  stop = () => {
    if (!this.playing) return;
    this.playing = false;
    this.wrong = false;

    if (this.mode() === 'nameSquare') {
      this.keyboardInput?.blur();
      if (this.keyboardInput) this.keyboardInput.value = '';
    }

    if (this.timeControl() === 'thirtySeconds') {
      this.updateScoreList();
      if (this.isAuth)
        xhr.text('/training/coordinate/score', {
          method: 'post',
          body: xhr.form({ mode: this.mode(), color: this.orientation, score: this.score }),
        });
    }

    this.chessground?.setShapes([]);
    this.chessground?.redrawAll();
    this.redraw();
  };

  updateScoreList = () => {
    // we only ever display the last 20 scores
    const scoreList = this.modeScores[this.mode()][this.orientation];
    if (scoreList.length >= 20) this.modeScores[this.mode()][this.orientation] = scoreList.slice(1, 20);
    this.modeScores[this.mode()][this.orientation].push(this.score);
    requestAnimationFrame(() => this.updateCharts());
  };

  updateCharts = () => {
    for (const color of ['white', 'black'] as Color[]) {
      const svgElement = document.getElementById(`${color}-sparkline`);
      if (!svgElement) continue;
      this.updateChart(svgElement as unknown as SVGSVGElement, color);
    }
  };

  updateChart = (svgElement: SVGSVGElement, color: Color) => {
    const parent = svgElement.parentElement as HTMLDivElement;
    svgElement.setAttribute('width', `${parent.offsetWidth}px`);
    sparkline(svgElement, this.modeScores[this.mode()][color], { interactive: true });
  };

  hasModeScores = (): boolean =>
    this.modeScores[this.mode()].white.length + this.modeScores[this.mode()].black.length > 0;

  handleCorrect = () => {
    this.score++;
    this.advanceCoordinates();
    this.wrong = false;
  };

  handleWrong = () => {
    clearTimeout(this.wrongTimeout);
    this.wrong = true;
    this.redraw();

    this.wrongTimeout = setTimeout(() => {
      this.wrong = false;
      this.redraw();
    }, 500);
  };

  onChessgroundSelect = (key: Key) => {
    if (!this.playing || this.mode() !== 'findSquare') return;

    if (key === this.currentKey) this.handleCorrect();
    else this.handleWrong();
  };

  onRadioInputKeyUp = (e: KeyboardEvent) => {
    // Mousetrap by default ignores key presses on inputs
    // when enter is pressed on a radio input, start training
    if (!this.playing && e.which === 13) this.start();
  };

  onVoice = (txt: string) => {
    if (this.playing) {
      if (txt.includes('stop')) {
        this.stop();
        return;
      }
      const words = txt.split(' ').map(w => rankWords[w] ?? w);
      if (this.currentKey && words.join('').includes(this.currentKey)) this.handleCorrect();
    } else if (txt.includes('start')) this.start();
  };

  onKeyboardInputKeyUp = (e: KeyboardEvent) => {
    // normalize input value
    const input = e.target as HTMLInputElement;
    input.value = input.value.toLowerCase().replace(/[^a-h1-8]/, '');

    if (!e.isTrusted || !this.playing) {
      input.value = '';
      if (e.which === 13) this.start();
    } else this.checkKeyboardInput();
  };

  checkKeyboardInput = () => {
    const input = this.keyboardInput;
    if (input.value.length === 1) {
      // clear input if it begins with anything other than a file
      input.value = input.value.replace(/[^a-h]/, '');
    } else if (input.value.length === 2 && input.value === this.currentKey) {
      input.value = '';
      this.handleCorrect();
    } else if (input.value.length === 2 && !input.value.match(/[a-h][1-8]/)) {
      // if they've entered e.g. "ab", change this to "b"
      input.value = input.value[1];
    } else if (input.value.length >= 2) {
      input.value = '';
      this.handleWrong();
    }
  };
}
