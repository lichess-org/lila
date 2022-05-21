import * as xhr from 'common/xhr';
import { snabModal } from 'common/modal';
import { bind, dataIcon, MaybeVNode } from 'common/snabbdom';
import { h } from 'snabbdom';
import AnalyseCtrl from './ctrl';

export default function view(ctrl: AnalyseCtrl): MaybeVNode {
  const { noarg } = ctrl.trans;
  return ctrl.shareGame()
    ? snabModal({
        class: 'share-game-modal',
        content: [
          h('h2', noarg('shareGame')),
          h('img', {
            attrs: {
              width: 200,
              src: `/game/export/gif/${ctrl.bottomColor()}/${ctrl.data.game.id}.gif`,
            },
          }),
          h('div.copy-game-url', [
            h('input#game-url', {
              attrs: {
                readonly: true,
                value: `${location.origin}/${ctrl.data.game.id}/${ctrl.bottomColor()}`,
              },
            }),
            h('a.button', {
              hook: bind('click', () => {
                $('input#game-url').each((_, input: HTMLInputElement) => input.select());
                document.execCommand('copy');
                $('.copy-success').addClass('visible');
              }),
              attrs: dataIcon(''),
            }),
          ]),
          h('div.copy-success', 'Link copied to clipboard'),
          h(
            'a.button.text',
            {
              attrs: {
                ...dataIcon(''),
                href: xhr.url(`/export/gif/${ctrl.node.fen.split(' ')[0]}`, {
                  color: ctrl.bottomColor(),
                  lastMove: ctrl.node.uci,
                }),
                target: '_blank',
                download: 'position.gif',
              },
              hook: bind('click', () => ctrl.shareGame(false)),
            },
            noarg('currentPosition')
          ),
          h(
            'a.button.text',
            {
              attrs: {
                ...dataIcon(''),
                href: `/game/export/gif/${ctrl.bottomColor()}/${ctrl.data.game.id}.gif`,
                target: '_blank',
                download: true,
              },
              hook: bind('click', () => ctrl.shareGame(false)),
            },
            noarg('gameAsGIF')
          ),
        ],
        onClose: () => ctrl.shareGame(false),
      })
    : null;
}
