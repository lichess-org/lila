import { h } from 'snabbdom'
import { Hooks } from 'snabbdom/hooks'
import { VNode } from 'snabbdom/vnode'
import AnalyseCtrl from '../../ctrl';
import { bind } from '../../util';
import { MaybeVNodes } from '../../interfaces';
import { throttle } from 'common';

export function running(ctrl: AnalyseCtrl): boolean {
  return !!ctrl.study && ctrl.study.data.chapter.gamebook && !ctrl.gamebookPlay();
}

export function render(ctrl: AnalyseCtrl): VNode {

  const study = ctrl.study!,
  isMyMove = ctrl.turnColor() === ctrl.data.orientation,
  isCommented = !!(ctrl.node.comments || []).find(c => c.text.length > 2),
  hasVariation = ctrl.tree.parentNode(ctrl.path).children.length > 1;

  let content: MaybeVNodes;

  function commentButton(text: string = 'comment') {
    return h('a.button.thin', {
      hook: bind('click', () => {
        study.commentForm.open(study.vm.chapterId, ctrl.path, ctrl.node);
      }, ctrl.redraw),
    }, text);
  }

  if (!ctrl.path) {
    if (isMyMove) content = [
      h('div.legend.todo', { class: { done: isCommented } }, [
        'Help the player find the initial move, with a ',
        commentButton(),
        '.'
      ]),
      renderHint(ctrl)
    ];
    else  content = [
      h('div.legend.todo', { class: { done: isCommented } }, [
        'Introduce the gamebook with a ',
        commentButton(),
        ', and put the opponent\'s first move on the board.'
      ])
    ];
  }
  else if (ctrl.onMainline) {
    if (isMyMove) content = [
      h('div.legend.todo', { class: { done: isCommented } }, [
        'Comment the opponent move, and help the player find the next move, with a ',
        commentButton(),
        '.'
      ]),
      renderHint(ctrl)
    ];
    else content = [
      h('div.legend', [
        'Reflect on the player\'s correct move, with a ',
        commentButton(),
        '; or leave empty to jump immediately to the next move.'
      ]),
      hasVariation ? null : h('div.legend', {
        attrs: { 'data-icon': '' }
      }, 'Add variation moves to explain why specific other moves are wrong.'),
      renderDeviation(ctrl)
    ];
  }
  else content = [
    h('div.legend.todo', { class: { done: isCommented } }, [
      'Explain why this move is wrong in a ',
      commentButton(),
      '.'
    ]),
    h('div.legend',
      'Or promote it as the mainline if it is the right move.')
  ];

  return h('div.gamebook_wrap', {
    hook: { insert: _ => window.lichess.loadCss('/assets/stylesheets/gamebook.edit.css') }
  }, [
    h('div.gamebook', content)
  ]);
}

function renderDeviation(ctrl: AnalyseCtrl): VNode {
  const field = 'deviation';
  return h('div.deviation.todo', { class: { done: nodeGamebookValue(ctrl.node, field).length > 2 } }, [
    h('label', {
      attrs: { for: 'gamebook-deviation' }
    }, 'When any other wrong move is played:'),
    h('textarea#gamebook-deviation', {
      attrs: { placeholder: 'Explain why all other moves are wrong' },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

function renderHint(ctrl: AnalyseCtrl): VNode {
  const field = 'hint';
  return h('div.hint', [
    h('label', {
      attrs: { for: 'gamebook-hint' }
    }, 'Optional, on-demand hint for the player:'),
    h('textarea#gamebook-hint', {
      attrs: { placeholder: 'Give the player a tip so they can find the right move' },
      hook: textareaHook(ctrl, field)
    })
  ]);
}

const saveNode = throttle(500, false, (ctrl: AnalyseCtrl, gamebook: Tree.Gamebook) => {
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
      function onChange() {
        const node = ctrl.node;
        node.gamebook = node.gamebook || {};
        node.gamebook[field] = el.value.trim();
        saveNode(ctrl, node.gamebook, 50);
      }
      el.onkeyup = el.onpaste = onChange;
      vnode.data!.path = ctrl.path;
    },
    postpatch(old: VNode, vnode: VNode) {
      if (old.data!.path !== ctrl.path) (vnode.elm as HTMLInputElement).value = value;
      vnode.data!.path = ctrl.path;
    }
  }
}
