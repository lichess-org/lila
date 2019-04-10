import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind } from '../util';
import { prop } from 'common';
import { renderIndexAndMove } from '../moveView';
import { StudyData, StudyChapterMeta } from './interfaces';

const baseUrl = 'https://lichess.org/study/';

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
  return h('div.study_share.underboard_form.box', [
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
    h('form.material.form', [
      h('div.form-group.little-margin-bottom', [
        h('input.has-value.autoselect', {
          attrs: {
            readonly: true,
            value: baseUrl + studyId
          }
        }),
        h('label.control-label', 'Study URL'),
        h('i.bar')
      ]),
      h('div.form-group', [
        h('input.has-value.autoselect', {
          attrs: {
            readonly: true,
            value: fullUrl
          }
        }),
        fromPly(ctrl),
        !isPrivate ? h('p.form-help.text', {
          attrs: { 'data-icon': '' }
        }, 'You can paste this in the forum to embed the chapter.') : null,
        h('label.control-label', 'Current chapter URL'),
        h('i.bar')
      ]),
      h('div.form-group', [
        h('input.has-value.autoselect', {
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
          }, 'Read more about embedding a study chapter'),
          h('label.control-label', 'Embed current chapter in your website or blog')
        ] : []).concat(h('i.bar'))
      ),
      h('div.fen', {
        attrs: { title: 'FEN - click to select' },
        hook: bind('click', e => {
          window.getSelection().selectAllChildren((e.target as HTMLElement))
        })
      }, ctrl.currentNode().fen)
    ])
  ]);
}
