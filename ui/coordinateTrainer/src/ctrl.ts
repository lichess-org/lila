import { sparkline } from '@fnando/sparkline';
import * as xhr from 'common/xhr';
import throttle from 'common/throttle';
import { Api as CgApi } from 'chessground/api';
import { ColorChoice, CoordinateTrainerConfig, Redraw } from './interfaces';

const orientationFromColorChoice = (colorChoice: ColorChoice): Color =>
  (colorChoice === 'random' ? ['white', 'black'][Math.round(Math.random())] : colorChoice) as Color;

const newKey = (oldKey: Key | ''): Key => {
  // disallow the previous coordinate's row or file from being selected
  const files = 'abcdefgh'.replace(oldKey[0], '');
  const rows = '12345678'.replace(oldKey[1], '');
  return (files[Math.floor(Math.random() * files.length)] + rows[Math.floor(Math.random() * files.length)]) as Key;
};

export const DURATION = 30 * 1000;
const TICK_DELAY = 50;

export default class CoordinateTrainerCtrl {
  config: CoordinateTrainerConfig;
  colorChoice: ColorChoice;
  orientation: Color;
  isAuth: boolean;
  playing = false;
  hasPlayed = false;
  trans: Trans;
  chessground: CgApi | undefined;
  redraw: Redraw;
  score = 0;
  currentKey: Key | '' = 'a1';
  nextKey: Key | '' = newKey('a1');
  timeLeft = DURATION;
  timeAtStart: Date;
  wrongTimeout: number;
  wrong: boolean;
  zen: boolean;

  constructor(config: CoordinateTrainerConfig, redraw: Redraw) {
    this.config = config;
    this.colorChoice = config.colorPref || 'random';
    this.orientation = orientationFromColorChoice(this.colorChoice);

    this.isAuth = document.body.hasAttribute('data-user');
    this.trans = lichess.trans(this.config.i18n);
    this.redraw = redraw;

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
      requestAnimationFrame(this.updateCharts);
    });

    window.Mousetrap.bind('z', () => lichess.pubsub.emit('zen'));

    $('#zentog').on('click', () => lichess.pubsub.emit('zen'));

    window.addEventListener('resize', () => requestAnimationFrame(this.updateCharts), true);
  }

  setColorChoice = (c: ColorChoice) => {
    if (this.colorChoice === c) return;
    this.colorChoice = c;
    this.setOrientation(orientationFromColorChoice(c));

    if (this.isAuth) {
      const colorChoiceToPref = { white: 1, random: 2, black: 3 };
      xhr.text('/training/coordinate/color', {
        method: 'post',
        body: xhr.form({ color: colorChoiceToPref[c] }),
      });
    }
  };

  setOrientation = (o: Color) => {
    this.orientation = o;
    if (this.chessground!.state.orientation !== o) this.chessground!.toggleOrientation();
    this.redraw();
  };

  start = () => {
    this.playing = true;
    this.hasPlayed = true;
    this.score = 0;
    this.timeLeft = DURATION;
    this.currentKey = '';
    this.nextKey = '';

    // In case random is selected, recompute orientation
    this.setOrientation(orientationFromColorChoice(this.colorChoice));

    setTimeout(() => {
      this.timeAtStart = new Date();
      this.advanceCoordinates();
      this.advanceCoordinates();
      this.tick();
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
    this.redraw();
  };

  stop = () => {
    this.playing = false;
    this.updateScoreList();
    requestAnimationFrame(() => this.updateCharts());
    this.redraw();

    if (this.isAuth) {
      xhr.text('/training/coordinate/score', {
        method: 'post',
        body: xhr.form({ color: this.orientation, score: this.score }),
      });
    }
  };

  updateScoreList = () => {
    // we only ever display the last 20 scores
    const scoreList = this.config.scores[this.orientation];
    if (scoreList.length >= 20) this.config.scores[this.orientation] = scoreList.slice(1, 20);
    this.config.scores[this.orientation].push(this.score);
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
    sparkline(svgElement, this.config.scores[color], { interactive: true });
  };

  onChessgroundSelect = (key: Key) => {
    if (!this.playing) return;

    if (key === this.currentKey) {
      this.score++;
      this.advanceCoordinates();
    } else {
      clearTimeout(this.wrongTimeout);
      this.wrong = true;
      this.wrongTimeout = setTimeout(() => {
        this.wrong = false;
      }, 500);
    }
  };
}
