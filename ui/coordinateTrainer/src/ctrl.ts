import { sparkline } from '@fnando/sparkline';
import * as xhr from 'common/xhr';
import throttle from 'common/throttle';
import { Api as CgApi } from 'chessground/api';
import { ColorChoice, TimeControl, CoordinateTrainerConfig, InputMethod, Mode, ModeScores, Redraw } from './interfaces';

const orientationFromColorChoice = (colorChoice: ColorChoice): Color =>
  (colorChoice === 'random' ? ['white', 'black'][Math.round(Math.random())] : colorChoice) as Color;

const newKey = (oldKey: Key | ''): Key => {
  // disallow the previous coordinate's row or file from being selected
  const files = 'abcdefgh'.replace(oldKey[0], '');
  const rows = '12345678'.replace(oldKey[1], '');
  return (files[Math.floor(Math.random() * files.length)] + rows[Math.floor(Math.random() * files.length)]) as Key;
};

const targetSvg = `
<g transform="translate(50, 50)">
  <rect class="current-target" fill="none" stroke-width="10" x="-50" y="-50" width="100" height="100" rx="5" />
</g>
`;

export const DURATION = 30 * 1000;
const TICK_DELAY = 50;

export default class CoordinateTrainerCtrl {
  chessground: CgApi | undefined;
  colorChoice: ColorChoice;
  config: CoordinateTrainerConfig;
  coordinateInputMethod: InputMethod;
  currentKey: Key | '' = 'a1';
  hasPlayed = false;
  isAuth: boolean;
  keyboardInput: HTMLInputElement;
  mode: Mode;
  modeScores: ModeScores;
  nextKey: Key | '' = newKey('a1');
  orientation: Color;
  playing = false;
  redraw: Redraw;
  score = 0;
  timeAtStart: Date;
  timeControl: TimeControl;
  timeLeft = DURATION;
  trans: Trans;
  wrong: boolean;
  wrongTimeout: number;
  zen: boolean;

  constructor(config: CoordinateTrainerConfig, redraw: Redraw) {
    this.config = config;
    this.colorChoice = (lichess.storage.get('coordinateTrainer.colorChoice') as ColorChoice) || 'random';
    this.timeControl =
      (lichess.storage.get('coordinateTrainer.timeControl') as TimeControl) ||
      (document.body.classList.contains('kid') ? 'untimed' : 'thirtySeconds');
    this.orientation = orientationFromColorChoice(this.colorChoice);
    this.modeScores = config.scores;

    // Assume a smaller viewport means mobile, and default to buttons
    const savedInputMethod = lichess.storage.get('coordinateTrainer.coordinateInputMethod');
    if (savedInputMethod) this.coordinateInputMethod = savedInputMethod as InputMethod;
    else this.coordinateInputMethod = window.innerWidth >= 980 ? 'text' : 'buttons';

    this.isAuth = document.body.hasAttribute('data-user');
    this.trans = lichess.trans(this.config.i18n);
    this.redraw = redraw;

    if (window.location.hash.length == 5) {
      this.mode = window.location.hash === '#name' ? 'nameSquare' : 'findSquare';
    } else {
      this.mode = lichess.storage.get('coordinateTrainer.mode') === 'nameSquare' ? 'nameSquare' : 'findSquare';
    }
    this.saveMode();

    const setZen = throttle(1000, zen =>
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
  }

  setMode = (m: Mode) => {
    if (this.mode === m) return;
    this.mode = m;
    this.saveMode();
    this.redraw();
    this.updateCharts();
  };

  saveMode = () => {
    window.location.hash = `#${this.mode.substring(0, 4)}`;
    lichess.storage.set('coordinateTrainer.mode', this.mode);
  };

  setColorChoice = (c: ColorChoice) => {
    if (this.colorChoice === c) return;
    this.colorChoice = c;
    this.setOrientation(orientationFromColorChoice(c));
    lichess.storage.set('coordinateTrainer.colorChoice', this.colorChoice);
  };

  setOrientation = (o: Color) => {
    this.orientation = o;
    if (this.chessground!.state.orientation !== o) this.chessground!.toggleOrientation();
    this.redraw();
  };

  setTimeControl = (c: TimeControl) => {
    if (this.timeControl === c) return;
    this.timeControl = c;
    lichess.storage.set('coordinateTrainer.timeControl', this.timeControl);
    this.redraw();
  };

  timeDisabled = () => this.timeControl === 'untimed';

  toggleInputMethod = () => {
    if (this.coordinateInputMethod === 'text') this.coordinateInputMethod = 'buttons';
    else this.coordinateInputMethod = 'text';
    this.redraw();
    lichess.storage.set('coordinateTrainer.coordinateInputMethod', this.coordinateInputMethod);
  };

  start = () => {
    this.playing = true;
    this.hasPlayed = true;
    this.score = 0;
    this.timeLeft = DURATION;
    this.currentKey = '';
    this.nextKey = '';

    // Redraw the chessground to remove the resize handle
    this.chessground?.redrawAll();

    // In case random is selected, recompute orientation
    this.setOrientation(orientationFromColorChoice(this.colorChoice));

    if (this.mode === 'nameSquare') this.keyboardInput.focus();

    setTimeout(() => {
      // Advance coordinates twice in order to get an entirely new set
      this.advanceCoordinates();
      this.advanceCoordinates();

      this.timeAtStart = new Date();
      if (!this.timeDisabled()) this.tick();
    }, 1000);
  };

  private tick = () => {
    const timeSpent = Math.min(DURATION, new Date().getTime() - +this.timeAtStart);
    this.timeLeft = DURATION - timeSpent;
    this.redraw();

    if (this.timeLeft > 0) setTimeout(this.tick, TICK_DELAY);
    else this.stop();
  };

  advanceCoordinates = () => {
    this.currentKey = this.nextKey;
    this.nextKey = newKey(this.nextKey);

    if (this.mode === 'nameSquare')
      this.chessground?.setShapes([{ orig: this.currentKey as Key, customSvg: targetSvg }]);

    this.redraw();
  };

  stop = () => {
    this.playing = false;
    this.wrong = false;

    if (this.mode === 'nameSquare') {
      this.keyboardInput.blur();
      this.keyboardInput.value = '';
    }

    if (this.timeControl === 'thirtySeconds') {
      this.updateScoreList();
      if (this.isAuth)
        xhr.text('/training/coordinate/score', {
          method: 'post',
          body: xhr.form({ mode: this.mode, color: this.orientation, score: this.score }),
        });
    }

    this.chessground?.setShapes([]);
    this.chessground?.redrawAll();
    this.redraw();
  };

  updateScoreList = () => {
    // we only ever display the last 20 scores
    const scoreList = this.modeScores[this.mode][this.orientation];
    if (scoreList.length >= 20) this.modeScores[this.mode][this.orientation] = scoreList.slice(1, 20);
    this.modeScores[this.mode][this.orientation].push(this.score);
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
    sparkline(svgElement, this.modeScores[this.mode][color], { interactive: true });
  };

  hasModeScores = (): boolean => this.modeScores[this.mode].white.length + this.modeScores[this.mode].black.length > 0;

  handleCorrect = () => {
    this.score++;
    this.advanceCoordinates();
    this.wrong = false;
  };

  handleWrong = () => {
    clearTimeout(this.wrongTimeout);
    this.wrong = true;
    this.wrongTimeout = setTimeout(() => {
      this.wrong = false;
    }, 500);
  };

  onChessgroundSelect = (key: Key) => {
    if (!this.playing || this.mode !== 'findSquare') return;

    if (key === this.currentKey) this.handleCorrect();
    else this.handleWrong();
  };

  onRadioInputKeyUp = (e: KeyboardEvent) => {
    // Mousetrap by default ignores key presses on inputs
    // when enter is pressed on a radio input, start training
    if (!this.playing && e.which === 13) this.start();
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
