import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, baseUrl } from '../util';
import { prop } from 'common';
import { renderIndexAndMove } from '../moveView';
import { StudyData, StudyChapterMeta } from './interfaces';

function fromPly(ctrl): VNode {
  var node = ctrl.currentNode();
  return h('div.ply-wrap', h('label.ply', [
    h('input', {
      attrs: { type: 'checkbox' },
      hook: bind('change', e => {
        ctrl.withPly((e.target as HTMLInputElement).checked);
      }, ctrl.redraw)
    }),
    ...ctrl.trans.vdom('startAtX', h('strong', renderIndexAndMove({
      withDots: true,
      showEval: false
    }, node)))
  ]));
}

export function ctrl(data: StudyData, currentChapter: () => StudyChapterMeta, currentNode: () => Tree.Node, redraw: () => void, trans: Trans) {
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
    redraw,
    trans
  }
}

export function view(ctrl): VNode {
  const studyId = ctrl.studyId, chapter = ctrl.chapter();
  let fullUrl = `${baseUrl()}/study/${studyId}/${chapter.id}`;
  let embedUrl = `${baseUrl()}/study/embed/${studyId}/${chapter.id}`;
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
      }, ctrl.trans.noarg('cloneStudy')) : null,
      h('a.button.text', {
        attrs: {
          'data-icon': 'x',
          href: '/study/' + studyId + '.pgn'
        }
      }, ctrl.trans.noarg('studyPgn')),
      h('a.button.text', {
        attrs: {
          'data-icon': 'x',
          href: '/study/' + studyId + '/' + chapter.id + '.pgn'
        }
      }, ctrl.trans.noarg('chapterPgn'))
    ]),
    h('form.form3', [
      h('div.form-group', [
        h('label.form-label', ctrl.trans.noarg('studyUrl')),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: `${baseUrl()}/study/${studyId}`
          }
        })
      ]),
      h('div.form-group', [
        h('label.form-label', ctrl.trans.noarg('currentChapterUrl')),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            value: fullUrl
          }
        }),
        fromPly(ctrl),
        !isPrivate ? h('p.form-help.text', {
          attrs: { 'data-icon': '' }
        }, ctrl.trans.noarg('youCanPasteThisInTheForumToEmbedTheChapter')) : null,
      ]),
      h('div.form-group', [
        h('label.form-label', ctrl.trans.noarg('embedThisChapter')),
        h('input.form-control.autoselect', {
          attrs: {
            readonly: true,
            disabled: isPrivate,
            value: !isPrivate ? '<iframe width=600 height=371 src="' + embedUrl + '" frameborder=0></iframe>' : ctrl.trans.noarg('onlyPublicStudiesCanBeEmbedded')
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
          }, ctrl.trans.noarg('readMoreAboutEmbeddingAStudyChapter'))
        ] : [])
      ),
      h('div.form-group', [
        h('label.form-label', ctrl.trans.noarg('fen')),
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
