import { MaybeVNodes, bind } from 'common/snabbdom';
import throttle from 'common/throttle';
import { Hooks, VNode, h } from 'snabbdom';
import * as control from '../../control';
import AnalyseCtrl from '../../ctrl';
import { iconTag } from '../../util';
import { requestIdleCallbackWithFallback } from 'common/common';
import { loadCssPath } from 'common/assets';
import { i18n } from 'i18n';

export function running(ctrl: AnalyseCtrl): boolean {
  return (
    !!ctrl.study &&
    ctrl.study.data.chapter.gamebook &&
    !ctrl.gamebookPlay() &&
    ctrl.study.vm.gamebookOverride !== 'analyse'
  );
}

export function render(ctrl: AnalyseCtrl): VNode {
  const study = ctrl.study!,
    isMyMove = ctrl.turnColor() === ctrl.data.orientation,
    isCommented = !!(ctrl.node.comments || []).find(c => c.text.length > 2),
    hasVariation = ctrl.tree.parentNode(ctrl.path).children.length > 1;

  let content: MaybeVNodes;

  const commentHook: Hooks = bind(
    'click',
    () => {
      study.commentForm.start(study.vm.chapterId, ctrl.path, ctrl.node);
      study.vm.toolTab('comments');
      requestIdleCallbackWithFallback(() => $('#comment-text').focus());
    },
    ctrl.redraw
  );

  if (!ctrl.path) {
    if (isMyMove)
      content = [
        h(
          'div.legend.todo.clickable',
          {
            hook: commentHook,
            class: { done: isCommented },
          },
          [iconTag('c'), h('p', i18n('study:initHelp'))]
        ),
        renderHint(ctrl),
      ];
    else
      content = [
        h(
          'div.legend.clickable',
          {
            hook: commentHook,
          },
          [iconTag('c'), h('p', i18n('study:introGamebook'))]
        ),
        h('div.legend.todo', { class: { done: !!ctrl.node.children[0] } }, [
          iconTag('G'),
          h('p', i18n('study:putFirstMove')),
        ]),
      ];
  } else if (ctrl.onMainline) {
    if (isMyMove)
      content = [
        h(
          'div.legend.todo.clickable',
          {
            hook: commentHook,
            class: { done: isCommented },
          },
          [iconTag('c'), h('p', i18n('study:explainMove'))]
        ),
        renderHint(ctrl),
      ];
    else
      content = [
        h(
          'div.legend.clickable',
          {
            hook: commentHook,
          },
          [iconTag('c'), h('p', i18n('study:reflectOnMove'))]
        ),
        hasVariation
          ? null
          : h(
              'div.legend.clickable',
              {
                hook: bind('click', () => control.prev(ctrl), ctrl.redraw),
              },
              [iconTag('G'), h('p', i18n('study:variationMoves'))]
            ),
        renderDeviation(ctrl),
      ];
  } else
    content = [
      h(
        'div.legend.todo.clickable',
        {
          hook: commentHook,
          class: { done: isCommented },
        },
        [iconTag('c'), h('p', i18n('study:explainWrongMove'))]
      ),
      h('div.legend', [h('p', i18n('study:orPromote'))]),
    ];

  return h(
    'div.gamebook-edit',
    {
      hook: {
        insert: _ => loadCssPath('analyse.gamebook.edit'),
      },
    },
    content
  );
}

function renderDeviation(ctrl: AnalyseCtrl): VNode {
  const field = 'deviation';
  return h('div.deviation', [
    h('div.legend.todo', { class: { done: nodeGamebookValue(ctrl.node, field).length > 2 } }, [
      iconTag('c'),
      h('p', i18n('study:otherMove')),
    ]),
    h('textarea', {
      attrs: { placeholder: i18n('study:explainAllWrong') },
      hook: textareaHook(ctrl, field),
    }),
  ]);
}

function renderHint(ctrl: AnalyseCtrl): VNode {
  const field = 'hint';
  return h('div.hint', [
    h('div.legend', [iconTag(''), h('p', i18n('study:hintOnDemand'))]),
    h('textarea', {
      attrs: {
        placeholder: i18n('study:playerTip'),
      },
      hook: textareaHook(ctrl, field),
    }),
  ]);
}

const saveNode = throttle(500, (ctrl: AnalyseCtrl, gamebook: Tree.Gamebook) => {
  ctrl.socket.send('setGamebook', {
    path: ctrl.path,
    ch: ctrl.study!.vm.chapterId,
    gamebook: gamebook,
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
    },
  };
}
