import { h } from 'snabbdom';
import { MaybeVNodes, onInsert } from 'common/snabbdom';
import { SetupCtrl } from '../ctrl';
import { fenInput } from './components/fenInput';
import { timePickerAndSliders } from './components/timePickerAndSliders';
import { colorButtons } from './components/colorButtons';
import { ratingView } from './components/ratingView';
import { localBots, type BotInfo } from 'libot';

const botInfos = Object.values(localBots);
export default function localContent(ctrl: SetupCtrl): MaybeVNodes {
  return [
    h('h2', 'Local Play'),
    h(
      'div.setup-content',
      /*{
        hook: onInsert(() => {
          document.addEventListener('keydown', (e: KeyboardEvent) => {
            console.log(e.key);
            if (e.key === 'ArrowUp') select(ctrl, 'prev');
            else if (e.key === 'ArrowDown') select(ctrl, 'next');
            else return;
            e.preventDefault();
          });
        }),
      },*/
      [botSelector(ctrl), fenInput(ctrl), timePickerAndSliders(ctrl, true), colorButtons(ctrl)],
    ),
    ratingView(ctrl),
  ];
}

function botSelector(ctrl: SetupCtrl) {
  if (lichess.blindMode) return null;
  return h('div#bot-select', [
    h(
      'div#bot-carousel',
      botInfos.map(bot => botItem(ctrl, bot)),
    ),
    h('div#bot-info', botInfo(ctrl, botInfos[0])),
  ]);
}

function botInfo(ctrl: SetupCtrl, bot: BotInfo) {
  ctrl;
  return h('div', [
    h('img', { attrs: { src: bot.image } }),
    h('div', [h('h2', bot.name), h('p', bot.description)]),
  ]);
}

function botItem(ctrl: SetupCtrl, bot: BotInfo) {
  ctrl;
  return h('div.libot', { hook: onInsert(el => el.addEventListener('click', () => select(ctrl, el))) }, [
    h('img', { attrs: { src: bot.image } }),
    h('div.label', bot.name),
  ]);
}

function select(ctrl: SetupCtrl, el: Element | 'next' | 'prev') {
  ctrl;
  const $bots = $('.libot');
  const bots: EleLoose[] = Array.from($bots.get());
  const selectedIndex = bots.findIndex(b => b.classList.contains('selected'));
  if (el === 'next') {
    if (selectedIndex < bots.length - 1) {
      bots[selectedIndex].classList.remove('selected');
      bots[selectedIndex + 1].classList.add('selected');
    }
  } else if (el === 'prev') {
    if (selectedIndex > 0) {
      bots[selectedIndex].classList.remove('selected');
      bots[selectedIndex - 1].classList.add('selected');
    }
  } else {
    $('.libot').removeClass('selected');
    $(el).addClass('selected');
  }
  const newIndex = bots.findIndex(b => b.classList.contains('selected'));
  const bot = botInfos[newIndex];
  const info = document.querySelector('#bot-info');
  info?.firstElementChild?.replaceWith(botInfo(ctrl, bot).elm!);
}
