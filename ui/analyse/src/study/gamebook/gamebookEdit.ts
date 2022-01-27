import * as control from '../../control';
import AnalyseCtrl from '../../ctrl';
import throttle from 'common/throttle';
import { bind, MaybeVNodes } from 'common/snabbdom';
import { iconTag } from '../../util';
import { h, Hooks, VNode } from 'snabbdom';

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
      lichess.requestIdleCallback(
        () =>
          $('#comment-text').each(function (this: HTMLTextAreaElement) {
            this.focus();
          }),
        500
      );
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
          [iconTag(''), h('p', 'Help the player find the initial move, with a comment.')]
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
          [iconTag(''), h('p', 'Introduce the gamebook with a comment')]
        ),
        h('div.legend.todo', { class: { done: !!ctrl.node.children[0] } }, [
          iconTag(''),
          h('p', "Put the opponent's first move on the board."),
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
          [iconTag(''), h('p', 'Explain the opponent move, and help the player find the next move, with a comment.')]
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
          [
            iconTag(''),
            h(
              'p',
              "You may reflect on the player's correct move, with a comment; or leave empty to jump immediately to the next move."
            ),
          ]
        ),
        hasVariation
          ? null
          : h(
              'div.legend.clickable',
              {
                hook: bind('click', () => control.prev(ctrl), ctrl.redraw),
              },
              [iconTag(''), h('p', 'Add variation moves to explain why specific other moves are wrong.')]
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
        [iconTag(''), h('p', 'Explain why this move is wrong in a comment')]
      ),
      h('div.legend', [h('p', 'Or promote it as the mainline if it is the right move.')]),
    ];

  return h(
    'div.gamebook-edit',
    {
      hook: { insert: _ => lichess.loadCssPath('analyse.gamebook.edit') },
    },
    content
  );
}

function renderDeviation(ctrl: AnalyseCtrl): VNode {
  const field = 'deviation';
  return h('div.deviation', [
    h('div.legend.todo', { class: { done: nodeGamebookValue(ctrl.node, field).length > 2 } }, [
      iconTag(''),
      h('p', 'When any other wrong move is played:'),
    ]),
    h('textarea', {
      attrs: { placeholder: 'Explain why all other moves are wrong' },
      hook: textareaHook(ctrl, field),
    }),
  ]);
}

function renderHint(ctrl: AnalyseCtrl): VNode {
  const field = 'hint';
  return h('div.hint', [
    h('div.legend', [iconTag(''), h('p', 'Optional, on-demand hint for the player:')]),
    h('textarea', {
      attrs: { placeholder: 'Give the player a tip so they can find the right move' },
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

function nodeGamebookValue(node: Tree.Node, field: 'deviation' | 'hint'): string {
  return (node.gamebook && node.gamebook[field]) || '';
}

function textareaHook(ctrl: AnalyseCtrl, field: 'deviation' | 'hint'): Hooks {
  const value = nodeGamebookValue(ctrl.node, field);
  return {
    insert(vnode: VNode) {
      const el = vnode.elm as HTMLInputElement;
      el.value = value;
      el.oninput = () => {
        const node = ctrl.node;
        node.gamebook = node.gamebook || {};
        node.gamebook[field] = el.value.trim();
        saveNode(ctrl, node.gamebook);
      };
      vnode.data!.path = ctrl.path;
    },
    postpatch(old: VNode, vnode: VNode) {
      if (old.data!.path !== ctrl.path) (vnode.elm as HTMLInputElement).value = value;
      vnode.data!.path = ctrl.path;
    },
  };
}
