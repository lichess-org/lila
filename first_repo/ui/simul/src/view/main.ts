import { h } from 'snabbdom';
import SimulCtrl from '../ctrl';
import * as util from './util';
import created from './created';
import { richHTML } from './text';
import results from './results';
import pairings from './pairings';

export default function (ctrl: SimulCtrl) {
  const handler = ctrl.data.isRunning ? started : ctrl.data.isFinished ? finished : created(showText);

  return h(
    'main.simul',
    {
      class: {
        'simul-created': ctrl.data.isCreated,
      },
    },
    [
      h('aside.simul__side', {
        hook: util.onInsert(el => {
          $(el).replaceWith(ctrl.opts.$side);
          ctrl.opts.chat && lichess.makeChat(ctrl.opts.chat);
        }),
      }),
      h(
        'div.simul__main.box',
        {
          hook: {
            postpatch() {
              lichess.miniGame.initAll();
            },
          },
        },
        handler(ctrl)
      ),
      h(
        'div.chat__members.none',
        {
          hook: util.onInsert(lichess.watchers),
        },
        h('span.list')
      ),
    ]
  );
}

const showText = (ctrl: SimulCtrl) =>
  ctrl.data.text
    ? h('div.simul-text', [
        h('p', {
          hook: richHTML(ctrl.data.text),
        }),
      ])
    : null;

const started = (ctrl: SimulCtrl) => [util.title(ctrl), showText(ctrl), results(ctrl), pairings(ctrl)];

const finished = (ctrl: SimulCtrl) => [
  h('div.box__top', [util.title(ctrl), h('div.box__top__actions', h('div.finished', ctrl.trans('finished')))]),
  showText(ctrl),
  results(ctrl),
  pairings(ctrl),
];
