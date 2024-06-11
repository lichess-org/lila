import * as util from '../util';
import * as scoring from '../score';
import { numberSpread } from 'common/number';
import { RunCtrl } from './runCtrl';
import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { hashNavigate } from '../hashRouting';

function makeStars(rank: number) {
  const stars = [];
  for (let i = 3; i > 0; i--) stars.push(h('div.star-wrap', rank <= i ? h('i.star') : null));
  return stars;
}

export default function (ctrl: RunCtrl) {
  const stage = ctrl.stage;
  const next = ctrl.getNext();
  const score = ctrl.stageScore();
  return h(
    'div.learn__screen-overlay',
    {
      hook: bind(
        'click',
        (e: MouseEvent) =>
          (e.target as HTMLElement).classList?.contains('learn__screen-overlay') && hashNavigate(),
      ),
    },
    h('div.learn__screen', [
      h('div.stars', makeStars(scoring.getStageRank(stage, score))),
      h('h1', ctrl.trans('stageXComplete', stage.id)),
      h(
        'span.score',
        ctrl.trans.vdom(
          'yourScore',
          h(
            'span',
            {
              hook: {
                insert: vnode => {
                  const el = vnode.elm as HTMLElement;
                  setTimeout(() => numberSpread(el, 50, 3000, 0)(score), 300);
                },
              },
            },
            '0',
          ),
        ),
      ),
      h('p', util.withLinebreaks(ctrl.trans.noarg(stage.complete))),
      h('div.buttons', [
        next
          ? h('a.next', { hook: bind('click', () => hashNavigate(next.id)) }, [
              ctrl.trans('nextX', ctrl.trans.noarg(next.title)) + ' ',
              h('i', { attrs: { 'data-icon': '' } }),
            ])
          : null,
        h(
          'a.back.text[data-icon=]',
          { hook: bind('click', () => hashNavigate()) },
          ctrl.trans.noarg('backToMenu'),
        ),
      ]),
    ]),
  );
}
