import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import { VNode } from 'snabbdom/vnode'
import AnalyseCtrl from '../../ctrl';
import { bind, iconTag } from '../../util';
import { MaybeVNodes } from '../../interfaces';
import throttle from 'common/throttle';
import * as control from '../../control';

export function running(ctrl: AnalyseCtrl): boolean {
  return !!ctrl.study && ctrl.study.data.chapter.gamebook &&
  !ctrl.gamebookPlay() && ctrl.study.vm.gamebookOverride !== 'analyse';
}

export function render(ctrl: AnalyseCtrl): VNode {

  const study = ctrl.study!,
  isMyMove = ctrl.turnColor() === ctrl.data.orientation,
  isCommented = !!(ctrl.node.comments || []).find(c => c.text.length > 2),
  hasVariation = ctrl.tree.parentNode(ctrl.path).children.length > 1;

  let content: MaybeVNodes;

  const commentHook: Hooks = bind('click', () => {
    study.commentForm.start(study.vm.chapterId, ctrl.path, ctrl.node);
    study.vm.toolTab('comments');
    window.lidraughts.requestIdleCallback(() => $('#comment-text').focus());
  }, ctrl.redraw);

  if (!ctrl.path) {
    if (isMyMove) content = [
      h('div.legend.todo.clickable', {
        hook: commentHook,
        class: { done: isCommented }
      }, [
        iconTag('c'),
        h('p', ctrl.trans.noarg('helpPlayerInitialMove'))
      ]),
      renderHint(ctrl)
    ];
    else content = [
      h('div.legend.clickable', {
        hook: commentHook
      }, [
        iconTag('c'),
        h('p', ctrl.trans.noarg('introduceLesson'))
      ]),
      h('div.legend.todo', { class: { done: !!ctrl.node.children[0] }}, [
        iconTag('G'),
        h('p', ctrl.trans.noarg('putOpponentsFirstMove'))
      ])
    ];
  }
  else if (ctrl.onMainline) {
    if (isMyMove) content = [
      h('div.legend.todo.clickable', {
        hook: commentHook,
        class: { done: isCommented }
      }, [
        iconTag('c'),
        h('p', ctrl.trans.noarg('explainOpponentMove'))
      ]),
      renderHint(ctrl)
    ];
    else content = [
      h('div.legend.clickable', {
        hook: commentHook,
      }, [
        iconTag('c'),
        h('p', ctrl.trans.noarg('reflectOnCorrectMove'))
      ]),
      hasVariation ? null : h('div.legend.clickable', {
        hook: bind('click', () => control.prev(ctrl), ctrl.redraw)
      }, [
        iconTag('G'),
        h('p', ctrl.trans.noarg('addVariationsWrongMoves'))
      ]),
      renderDeviation(ctrl)
    ];
  }
  else content = [
    h('div.legend.todo.clickable', {
      hook: commentHook,
      class: { done: isCommented }
    }, [
      iconTag('c'),
      h('p', ctrl.trans.noarg('explainWhyThisMoveIsWrong'))
    ]),
    h('div.legend', [
      h('p', ctrl.trans.noarg('orPromoteItToMainline'))
    ])
  ];

  return h('div.gamebook_wrap', {
    hook: { insert: _ => window.lidraughts.loadCssPath('analyse.gamebook.edit') }
  }, [
    h('div.gamebook', content)
  ]);
}

function renderDeviation(ctrl: AnalyseCtrl): VNode {
  const field = 'deviation';
  return h('div.deviation', [
    h('div.legend.todo', { class: { done: nodeGamebookValue(ctrl.node, field).length > 2 } }, [
      iconTag('c'),
      h('p', ctrl.trans.noarg('anyOtherWrongMove'))
    ]),
    h('textarea', {
      attrs: { placeholder: ctrl.trans.noarg('explainWhyOtherMovesWrong') },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

function renderHint(ctrl: AnalyseCtrl): VNode {
  const field = 'hint';
  return h('div.hint', [
    h('div.legend', [
      iconTag(''),
      h('p', ctrl.trans.noarg('optionalHint'))
    ]),
    h('textarea', {
      attrs: { placeholder: ctrl.trans.noarg('giveThePlayerAHint') },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

const saveNode = throttle(500, (ctrl: AnalyseCtrl, gamebook: Tree.Gamebook) => {
  ctrl.socket.send('setGamebook', {
    path: ctrl.path,
    ch: ctrl.study!.vm.chapterId,
    gamebook: gamebook
  });
  ctrl.redraw();
});

function nodeGamebookValue(node: Tree.Node, field: string): string {
  return (node.gamebook && node.gamebook[field]) || '';
}

function textareaHook(ctrl: AnalyseCtrl, field: string): Hooks {
  const value = nodeGamebookValue(ctrl.node, field);
  return {
    insert(vnode: VNode) {
      const el = vnode.elm as HTMLInputElement;
      el.value = value;
      el.onkeyup = el.onpaste = () => {
        const node = ctrl.node;
        node.gamebook = node.gamebook || {};
        node.gamebook[field] = el.value.trim();
        saveNode(ctrl, node.gamebook, 50);
      };
      vnode.data!.path = ctrl.path;
    },
    postpatch(old: VNode, vnode: VNode) {
      if (old.data!.path !== ctrl.path) (vnode.elm as HTMLInputElement).value = value;
      vnode.data!.path = ctrl.path;
    }
  }
}
