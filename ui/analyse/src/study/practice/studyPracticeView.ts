import { h, thunk } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { plural, bind, spinner, innerHTML, enrichText, option } from '../../util';
import { StudyCtrl } from '../interfaces';
import { StudyPracticeData, StudyPracticeCtrl } from './interfaces';
import { boolSetting } from '../../boolSetting';

function selector(data: StudyPracticeData) {
  return h('select.selector', {
    hook: bind('change', e => {
      location.href = '/practice/' + (e.target as HTMLInputElement).value;
    })
  }, [
    h('option', {
      attrs: { disabled: true, selected: true }
    }, 'Practice list'),
    ...data.structure.map(function(section) {
      return h('optgroup', {
        attrs: { label: section.name }
      }, section.studies.map(function(study) {
        return option(
          section.id + '/' + study.slug + '/' + study.id,
          '',
          study.name);
      }));
    })
  ]);
}

function renderGoal(practice: StudyPracticeCtrl, inMoves: number) {
  const goal = practice.goal();
  switch (goal.result) {
    case 'mate':
      return 'Checkmate the opponent';
    case 'mateIn':
      return 'Checkmate the opponent in ' + plural('move', inMoves);
    case 'drawIn':
      return 'Hold the draw for ' + plural('more move', inMoves);
    case 'equalIn':
      return 'Equalize in ' + plural('move', inMoves);
    case 'evalIn':
      if (practice.isWhite() === (goal.cp! >= 0))
        return 'Get a winning position in ' + plural('move', inMoves);
      return 'Defend for ' + plural('move', inMoves);
    case 'promotion':
      return 'Safely promote your pawn';
  }
}

export function underboard(ctrl: StudyCtrl): VNode {
  if (ctrl.vm.loading) return h('div.feedback', spinner());
  const p = ctrl.practice!;
  switch (p.success()) {
    case true:
      const next = ctrl.nextChapter();
      return h('a.feedback.win', next ? {
        hook: bind('click', p.goToNext)
      } : {
        attrs: { href: '/practice' }
      }, [
        h('span', 'Success!'),
        ctrl.nextChapter() ? 'Go to next exercise' : 'Back to practice menu'
      ]);
 case false:
   return h('a.feedback.fail', {
     hook: bind('click', p.reset, ctrl.redraw)
   }, [
     h('span', [renderGoal(p, p.goal().moves!)]),
     h('strong', 'Click to retry')
   ]);
 default:
   return h('div', [
     h('div.feedback.ongoing', [
       h('div.goal', [renderGoal(p, p.goal().moves! - p.nbMoves())]),
       p.comment() ? h('div.comment', {
         hook: innerHTML(p.comment(), text => enrichText(text!, true, false))
       }) : null
     ]),
     boolSetting({
       name: 'Load next exercise immediately',
       id: 'autoNext',
       checked: p.autoNext(),
       change: p.autoNext
     }, ctrl.trans, ctrl.redraw)
   ]);
  }
}

export function main(ctrl: StudyCtrl): VNode {

  const current = ctrl.currentChapter(),
  data = ctrl.practice!.data;

  return h('div.side_box.study_box', [
    h('div.title', [
      h('i.practice.icon.' + data.study.id),
      h('div.text', [
        h('h1', data.study.name),
        h('em', data.study.desc)
      ])
    ]),
    h('div.list.chapters', {
      hook: bind('click', e => {
        e.preventDefault();
        const target = e.target as HTMLElement;
        const id = (target.parentNode as HTMLElement).getAttribute('data-id') || target.getAttribute('data-id');
        if (id) ctrl.setChapter(id, true);
        return false;
      })
    }, ctrl.chapters.list().map(function(chapter) {
      const loading = ctrl.vm.loading && chapter.id === ctrl.vm.nextChapterId;
      const active = !ctrl.vm.loading && current && current.id === chapter.id;
      const completion = data.completion[chapter.id] ? 'done' : 'ongoing';
      return [
        h('a.elem.chapter', {
          key: chapter.id,
          attrs: {
            href: data.url + '/' + chapter.id,
            'data-id': chapter.id
          },
          class: { active, loading }
        }, [
          h('span.status.' + completion, {
            attrs: {
              'data-icon': ((loading || active) && completion === 'ongoing') ? 'G' : 'E'
            }
          }),
          h('h3', chapter.name)
        ])
      ];
    }).reduce(function(a, b) { return a.concat(b); }, [])),
    h('div.finally', [
      h('a.back', {
        attrs: {
          'data-icon': 'I',
          href: '/practice',
          title: 'More practice'
        }
      }),
      thunk('select.selector', selector, [data])
    ])
  ]);
}
