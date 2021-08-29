import m from '../mithrilFix';
import * as util from '../util';
import * as scoring from '../score';
import { numberSpread } from 'common/number';
import { Ctrl } from './runCtrl';

function makeStars(rank: number) {
  const stars = [];
  for (let i = 3; i > 0; i--) stars.push(m('div.star-wrap', rank <= i ? m('i.star') : null));
  return stars;
}

export default function (ctrl: Ctrl) {
  const stage = ctrl.stage;
  const next = ctrl.getNext();
  const score = ctrl.stageScore();
  return m(
    'div.learn__screen-overlay',
    {
      onclick: function (e: MouseEvent) {
        if ((e.target as HTMLElement).classList?.contains('learn__screen-overlay')) m.route('/');
      },
    },
    m('div.learn__screen', [
      m('div.stars', makeStars(scoring.getStageRank(stage, score))),
      m('h1', ctrl.trans('stageXComplete', stage.id)),
      m('span.score', [
        ctrl.trans.noarg('yourScore') + ': ',
        m(
          'span',
          {
            config: function (el: HTMLElement, isUpdate: boolean) {
              if (!isUpdate)
                setTimeout(function () {
                  numberSpread(el, 50, 3000, 0)(score);
                }, 300);
            },
          },
          '0'
        ),
      ]),
      m('p', util.withLinebreaks(ctrl.trans.noarg(stage.complete))),
      m('div.buttons', [
        next
          ? m(
              'a.next',
              {
                href: '/' + next.id,
                config: m.route,
              },
              [ctrl.trans.noarg('next') + ': ', ctrl.trans.noarg(next.title) + ' ', m('i[data-icon=]')]
            )
          : null,
        m(
          'a.back.text[data-icon=]',
          {
            href: '/',
            config: m.route,
          },
          ctrl.trans.noarg('backToMenu')
        ),
      ]),
    ])
  );
}
