import { h } from 'snabbdom'
import AnalyseController from '../../../ctrl';
import { nodeFullName, plyColor } from '../../../util';
import { VNode } from 'snabbdom/vnode'
import { throttle } from 'common';

export default function(ctrl: AnalyseController): VNode {

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
      ctrl.path ? (
        ctrl.onMainline ? (
          plyColor(ctrl.node.ply) === ctrl.data.orientation ? renderDeviation(ctrl) : renderOpponentMove()
        ) : renderVariation()
      ) : h('div.legend',
        'Help the player find the initial move, with a comment'
      )
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
});

function renderDeviation(ctrl: AnalyseController): VNode {
  const node = ctrl.node,
  path = ctrl.path,
  gamebook: Tree.Gamebook = node.gamebook || {},
  deviation = gamebook.deviation || '';
  return h('div.deviation', [
    h('label', {
      attrs: { for: 'gamebook-deviation' }
    }, 'When any other move is played:'),
    h('textarea#gamebook-deviation', {
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

function renderOpponentMove(): VNode {
  return h('div.legend', [
    'Comment the opponent move'
  ]);
}
