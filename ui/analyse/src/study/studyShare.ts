import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind } from '../util';
import { prop } from 'common';
import { renderIndexAndMove } from '../moveView';
import { StudyData, StudyChapterMeta } from './interfaces';

const baseUrl = `${window.location.protocol}//${window.location.host}/study/`;

function fromPly(ctrl): VNode {
  var node = ctrl.currentNode();
  return h('div.ply-wrap', h('label.ply', [
    h('input', {
      attrs: { type: 'checkbox' },
      hook: bind('change', e => {
        ctrl.withPly((e.target as HTMLInputElement).checked);
      }, ctrl.redraw)
    }),
    'Start at ',
    h('strong', renderIndexAndMove({
      withDots: true,
      showEval: false
    }, node))
  ]));
}

export function ctrl(data: StudyData, currentChapter: () => StudyChapterMeta, currentNode: () => Tree.Node, redraw: () => void) {
  const withPly = prop(false);
  return {
    studyId: data.id,
    chapter: currentChapter,
    isPrivate() {
      return data.visibility === 'private';
    },
    currentNode,
    withPly,
    cloneable: data.features.cloneable,
    redraw
  }
}

export function view(ctrl): VNode {
  const studyId = ctrl.studyId, chapter = ctrl.chapter();
  let fullUrl = baseUrl + studyId + '/' + chapter.id;
  let embedUrl = baseUrl + 'embed/' + studyId + '/' + chapter.id;
  const isPrivate = ctrl.isPrivate();
  if (ctrl.withPly()) {
    const p = ctrl.currentNode().ply;
    fullUrl += '#' + p;
    embedUrl += '#' + p;
  }
  return h('div.study__share', [
    h('div.downloads', [
      ctrl.cloneable ? h('a.button.text', {
        attrs: {
          'data-icon': '4',
          href: '/study/' + studyId + '/clone'
        }
      }, 'Clone') : null,
      h('a.button.text', {
        attrs: {
          'data-icon': 'x',
          href: '/study/' + studyId + '.pgn'
        }
      }, 'Study PGN'),
      h('a.button.text', {
        attrs: {
          'data-icon': 'x',
          href: '/study/' + studyId + '/' + chapter.id + '.pgn'
        }
      }, 'Chapter PGN')
    ]),
    h('form.form3', [
      h('div.form-group', [
        h('label.form-label', 'Study URL'),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: baseUrl + studyId
          }
        })
      ]),
      h('div.form-group', [
        h('label.form-label', 'Current chapter URL'),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: fullUrl
          }
        }),
        fromPly(ctrl),
        !isPrivate ? h('p.form-help.text', {
          attrs: { 'data-icon': '' }
        }, 'You can paste this in the forum to embed the chapter.') : null,
      ]),
      h('div.form-group', [
        h('label.form-label', 'Embed this chapter in your website or blog'),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            disabled: isPrivate,
            value: !isPrivate ? '<iframe width=600 height=371 src="' + embedUrl + '" frameborder=0></iframe>' : 'Only public studies can be embedded!'
          }
        })
      ].concat(
        !isPrivate ? [
          fromPly(ctrl),
          h('a.form-help.text', {
            attrs: {
              href: '/developers#embed-study',
              target: '_blank',
              'data-icon': ''
            }
          }, 'Read more about embedding a study chapter.')
        ] : [])
      ),
      h('div.form-group', [
        h('label.form-label', 'FEN'),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: ctrl.currentNode().fen
          },
        })
      ])
    ])
  ]);
}
