import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { plural, bind, spinner } from '../../util';
import { enrichText } from '../studyComments';
import { StudyController } from '../interfaces';

// #TODO render only once
function selector(data) {
  // if (!firstRender && m.redraw.strategy() === 'diff') return {
  //   subtree: 'retain'
  // };
  return h('select.selector', {
    hook: bind('change', e => {
      location.href = '/practice/' + (e.target as HTMLInputElement).value;
    })
  }, [
    h('option', {
      attrs: { disabled: true, selected: true }
    }, 'Practice list'),
    data.structure.map(function(section) {
      return h('optgroup', {
        attrs: { label: section.name }
      }, section.studies.map(function(study) {
        return h('option', {
          attrs: { value: section.id + '/' + study.slug + '/' + study.id }
        }, study.name);
      }));
    })
  ]);
}

function renderGoal(practice, inMoves: number) {
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
      if (practice.isWhite() === (goal.cp >= 0))
        return 'Get a winning position in ' + plural('move', inMoves);
      return 'Defend for ' + plural('move', inMoves);
    case 'promotion':
      return 'Safely promote your pawn';
  }
}

export function underboard(ctrl: StudyController): VNode {
  if (ctrl.vm.loading) return h('div.feedback', spinner());
  const p = ctrl.practice!;
  switch (p.success()) {
    case true:
      return h('a.feedback.win', {
        attrs: { href: '/practice' }
      }, [
        h('span', 'Success!'),
        p.nextChapter() ? null : 'Back to practice menu'
      ]);
 case false:
   return h('a.feedback.fail', {
     hook: bind('click', p.reset)
   }, [
     h('span', [renderGoal(p, p.goal().moves)]),
     h('strong', 'Click to retry')
   ]);
 default:
   return h('div.feedback.ongoing', [
     h('div.goal', [renderGoal(p, p.goal().moves - p.nbMoves())]),
     p.comment() ? h('div.comment', enrichText(p.comment(), true)) : null
   ]);
  }
}

export function main(ctrl: StudyController): VNode[] {

  const current = ctrl.currentChapter();
  const data = ctrl.practice.data;

  return [
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
    }, [
      ctrl.chapters.list().map(function(chapter) {
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
      })
    ]),
    h('div.finally', [
      h('a.back', {
        attrs: {
          'data-icon': 'I',
          href: '/practice',
          title: 'More practice'
        }
      }),
      selector(data)
    ])
  ];
}
