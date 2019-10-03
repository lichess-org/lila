import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import * as created from './created';
import * as started from './started';
import * as finished from './finished';
import { onInsert, bind } from './util';
import TournamentController from '../ctrl';
import { MaybeVNodes } from '../interfaces';

export default function(ctrl: TournamentController) {
  let handler: {
    name: string;
    main(ctrl: TournamentController): MaybeVNodes;
    table(ctrl: TournamentController): VNode | undefined;
  };
  if (ctrl.data.isFinished) handler = finished;
  else if (ctrl.data.isStarted) handler = started;
  else handler = created;

  return h('main.' + ctrl.opts.classes, [
    h('aside.analyse__side', {
      hook: onInsert(el => {
        $(el).replaceWith(ctrl.opts.$side);
        ctrl.opts.chat && window.lichess.makeChat(ctrl.opts.chat);
      })
    }),
    h('div.tour__underchat', {
      hook: onInsert(el => {
        $(el).replaceWith($('.tour__underchat.none').removeClass('none'));
      })
    }),
    handler.table(ctrl),
    h('div.tour__main',
      h('div.box.' + handler.name, {
        class: { 'tour__main-finished': ctrl.data.isFinished }
      }, handler.main(ctrl))
    ),
    ctrl.opts.chat ? h('div.chat__members.none', [
      h('span.number', '\xa0'), ' ', ctrl.trans.noarg('spectators'), ' ', h('span.list')
    ]) : null,
    ctrl.joinWithTeamSelector ? joinWithTeamSelector(ctrl) : null
  ]);
}

function joinWithTeamSelector(ctrl: TournamentController) {
  const onClose = () => {
    ctrl.joinWithTeamSelector = false;
    ctrl.redraw();
  };
  return h('div#modal-overlay', {
    hook: bind('click', onClose)
  }, [
    h('div#modal-wrap.team-battle__choice', {
      hook: onInsert(el => {
        el.addEventListener('click', e => e.stopPropagation());
      })
    }, [
      h('span.close', {
        attrs: { 'data-icon': 'L' },
        hook: bind('click', onClose)
      }),
      h('div', [
        h('h2', "Pick your team"),
        h('p', "Which team will you represent in this battle?"),
        ...ctrl.data.teamBattle!.joinWithTeams.map(t => h('a.button', {
          hook: bind('click', () => ctrl.join(undefined, t.id), ctrl.redraw)
        }, t.name))
      ])
    ])
  ]);
}
