import { makeChat } from 'chat';
import { initAll } from 'common/mini-board';
import { richHTML } from 'common/rich-text';
import { onInsert } from 'common/snabbdom';
import { i18n } from 'i18n';
import { h } from 'snabbdom';
import type SimulCtrl from '../ctrl';
import created from './created';
import pairings from './pairings';
import results from './results';
import * as util from './util';

export default function (ctrl: SimulCtrl) {
  const handler = ctrl.data.isRunning
    ? started
    : ctrl.data.isFinished
      ? finished
      : created(showText);

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
            makeChat(ctrl.opts.chat);
          }
        }),
      }),
      h(
        'div.simul__main.box',
        {
          hook: {
            postpatch() {
              initAll();
            },
          },
        },
        handler(ctrl),
      ),
      h('div.chat__members.none', {
        hook: onInsert(el => {
          $(el).watchers();
        }),
      }),
    ],
  );
}

const showText = (ctrl: SimulCtrl) =>
  ctrl.data.text.length > 0
    ? h('div.simul-text', [
        h('p', {
          hook: richHTML(ctrl.data.text),
        }),
      ])
    : undefined;

const started = (ctrl: SimulCtrl) => [
  util.title(ctrl),
  showText(ctrl),
  results(ctrl),
  pairings(ctrl),
];

const finished = (ctrl: SimulCtrl) => [
  h('div.box__top', [
    util.title(ctrl),
    h('div.box__top__actions', h('div.finished', i18n('finished'))),
  ]),
  showText(ctrl),
  results(ctrl),
  pairings(ctrl),
];
