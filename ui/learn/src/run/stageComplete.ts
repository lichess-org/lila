import { withLinebreaks } from '../util';
import { getStageRank } from '../score';
import { numberSpread } from 'common/number';
import type { RunCtrl } from './runCtrl';
import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import { hashNavigate } from '../hashRouting';
import * as licon from 'common/licon';

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
        e => (e.target as HTMLElement).classList?.contains('learn__screen-overlay') && hashNavigate(),
      ),
    },
    h('div.learn__screen', [
      h('div.stars', makeStars(getStageRank(stage, score))),
      h('h1', i18n.learn.stageXComplete(stage.id)),
      h(
        'span.score',
        i18n.site.yourScore.asArray(
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
      h('p', withLinebreaks(stage.complete)),
      h('div.buttons', [
        next
          ? h('a.next', { hook: bind('click', () => hashNavigate(next.id)) }, [
              i18n.learn.nextX(next.title) + ' ',
              h('i', { attrs: { 'data-icon': licon.GreaterThan } }),
            ])
          : null,
        h(
          `a.back.text[data-icon=${licon.LessThan}]`,
          { hook: bind('click', () => hashNavigate()) },
          i18n.learn.backToMenu,
        ),
      ]),
    ]),
  );
}
