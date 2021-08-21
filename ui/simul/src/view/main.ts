import { h } from 'snabbdom';
import { onInsert } from 'common/snabbdom';
import SimulCtrl from '../ctrl';
import * as util from './util';
import created from './created';
import { richHTML } from 'common/richText';
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
        hook: onInsert(el => {
          $(el).replaceWith(ctrl.opts.$side);
          if (ctrl.opts.chat) {
            ctrl.opts.chat.data.hostId = ctrl.data.host.id;
            lichess.makeChat(ctrl.opts.chat);
          }
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
      h('div.chat__members.none', {
        hook: onInsert(lichess.watchers),
      }),
    ]
  );
}

const showText = (ctrl: SimulCtrl) =>
  h('div.simul-text', [
    h('p', {
      hook: richHTML(ctrl.data.text),
    }),
  ]);

const started = (ctrl: SimulCtrl) => [util.title(ctrl), showText(ctrl), results(ctrl), pairings(ctrl)];

const finished = (ctrl: SimulCtrl) => [
  h('div.box__top', [util.title(ctrl), h('div.box__top__actions', h('div.finished', ctrl.trans('finished')))]),
  showText(ctrl),
  results(ctrl),
  pairings(ctrl),
];
