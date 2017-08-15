import { h } from 'snabbdom'
import AnalyseController from '../../../ctrl';
import { nodeFullName, bind } from '../../../util';
import { MaybeVNodes } from '../../../interfaces';
import { VNode } from 'snabbdom/vnode'
import { throttle } from 'common';

export default function(ctrl: AnalyseController): VNode {

  const isMyMove = ctrl.turnColor() === ctrl.data.orientation,
  isCommented = !!(ctrl.node.comments || []).find(c => c.text.length > 2);

  let content: MaybeVNodes;

  function commentButton(text: string = 'comment') {
    return h('a.button.thin', {
      hook: bind('click', () => {
        ctrl.study.commentForm.open(ctrl.study.vm.chapterId, ctrl.path, ctrl.node);
      }, ctrl.redraw),
    }, text);
  }

  if (!ctrl.path) content = [
    h('div.legend.todo', { class: { done: isCommented } }, [
      'Help the player find the initial move, with a ',
      commentButton(),
      '.'
    ])
  ];
  else if (ctrl.onMainline) {
    if (isMyMove) content = [
      h('div.legend.todo', { class: { done: isCommented } }, [
        'Comment the opponent move, and help the player find the next move, with a ',
        commentButton(),
        '.'
      ])
    ];
    else content = [
      h('div.legend.todo', { class: { done: isCommented } }, [
        'Congratulate the player for this correct move, with a ',
        commentButton(),
        '.'
      ]),
      h('div.legend', 'Add variation moves to explain why specific other moves are wrong.'),
      renderDeviation(ctrl)
    ];
  }
  else content = [
    renderVariation()
  ];

  return h('div.gamebook', {
    hook: {
      insert: _ => {
        window.lichess.loadCss('/assets/stylesheets/gamebook.css')
      }
    }
  }, [
    h('div.editor', [
      h('span.title', [
        'Gamebook editor: ',
        nodeFullName(ctrl.node)
      ]),
      ...content
    ])
  ]);
}

let prevPath: Tree.Path;

const saveNode = throttle(500, false, (ctrl: AnalyseController, gamebook: Tree.Gamebook) => {
  ctrl.socket.send('setGamebook', {
    path: ctrl.path,
    ch: ctrl.study!.vm.chapterId,
    gamebook: gamebook
  });
  ctrl.redraw();
});

function renderDeviation(ctrl: AnalyseController): VNode {
  const node = ctrl.node,
  path = ctrl.path,
  gamebook: Tree.Gamebook = node.gamebook || {},
  deviation = gamebook.deviation || '';
  return h('div.deviation.todo', { class: { done: deviation.length > 2 } }, [
    h('label', {
      attrs: { for: 'gamebook-deviation' }
    }, 'Or, when any other move is played:'),
    h('textarea#gamebook-deviation', {
      attrs: {
        placeholder: 'Explain why all other moves are wrong'
      },
      hook: {
        insert(vnode: VNode) {
          const el = vnode.elm as HTMLInputElement;
          el.value = deviation;
          function onChange() {
            node.gamebook = node.gamebook || {};
            node.gamebook.deviation = el.value.trim();
            saveNode(ctrl, node.gamebook, 50);
          }
          el.onkeyup = el.onpaste = onChange;
          prevPath = path;
        },
        postpatch(_, vnode: VNode) {
          if (prevPath !== path) {
            (vnode.elm as HTMLInputElement).value = deviation;
            prevPath = path;
          }
        }
      }
    })
  ]);
}

function renderVariation(): VNode {
  return h('div.legend', [
    'Explain why this move is wrong in a comment,',
    h('br'),
    'or promote it as the mainline if it is the right move.'
  ]);
}
