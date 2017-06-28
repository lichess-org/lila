import { h } from 'snabbdom'
import * as dialog from './dialog';
import { bind } from '../util';
import { prop } from 'common';
import { renderIndexAndMove } from '../moveView';

const baseUrl = 'https://lichess.org/study/';

function fromPly(ctrl) {
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

export function ctrl(data, currentChapter, currentNode, redraw: () => void) {
  const open = prop(false);
  const withPly = prop(false);
  return {
    open,
    toggle: function() {
      open(!open());
    },
    studyId: data.id,
    chapter: currentChapter,
    isPublic: function() {
      return data.visibility === 'public';
    },
    currentNode,
    withPly,
    cloneable: data.features.cloneable,
    redraw
  }
}

export function view(ctrl) {
  if (!ctrl.open()) return;
  const studyId = ctrl.studyId;
  const chapter = ctrl.chapter();
  let fullUrl = baseUrl + studyId + '/' + chapter.id;
  let embedUrl = baseUrl + 'embed/' + studyId + '/' + chapter.id;
  const isPublic = ctrl.isPublic();
  if (ctrl.withPly()) {
    const p = ctrl.currentNode().ply;
    fullUrl += '#' + p;
    embedUrl += '#' + p;
  }
  return dialog.form({
    onClose: function() {
      ctrl.open(false);
    },
    content: [
      h('h2', 'Share & export'),
      h('form.material.form.share', [
        h('div.form-group', [
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
          isPublic ? h('p.form-help.text', {
            attrs: { 'data-icon': '' }
          }, 'You can paste this in the forum to embed the chapter.') : null,
          h('label.control-label', 'Current chapter URL'),
          h('i.bar')
        ]),
        h('div.form-group', [
          h('input.has-value.autoselect', {
            attrs: {
              readonly: true,
              disabled: !isPublic,
              value: isPublic ? '<iframe width=600 height=371 src="' + embedUrl + '" frameborder=0></iframe>' : 'Only public studies can be embedded!'
            }
          }),
          isPublic ? [
            fromPly(ctrl),
            h('a.form-help.text', {
              attrs: {
                href: '/developers#embed-study',
                target: '_blank',
                'data-icon': ''
              }
            }, 'Read more about embedding a study chapter'),
            h('label.control-label', 'Embed current chapter in your website or blog')
          ] : null,
          h('i.bar')
        ]),
        h('div.downloads', [
          ctrl.cloneable ? h('a.button.text', {
            attrs: {
              'data-icon': '',
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
        ])
      ])
    ]
  });
}
