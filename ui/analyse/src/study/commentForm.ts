import { prop } from 'common';
import { onInsert } from 'common/snabbdom';
import throttle from 'common/throttle';
import { h, VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { currentComments, isAuthorObj } from './studyComments';

interface Current {
  chapterId: string;
  path: Tree.Path;
  node: Tree.Node;
}

export class CommentForm {
  current = prop<Current | null>(null);
  opening = prop(false);
  constructor(readonly root: AnalyseCtrl) {}

  submit = (text: string) => this.current() && this.doSubmit(text);

  doSubmit = throttle(500, (text: string) => {
    const cur = this.current();
    if (cur) this.root.study!.makeChange('setComment', { ch: cur.chapterId, path: cur.path, text });
  });

  start = (chapterId: string, path: Tree.Path, node: Tree.Node): void => {
    this.opening(true);
    this.current({ chapterId, path, node });
    this.root.userJump(path);
  };

  onSetPath = (chapterId: string, path: Tree.Path, node: Tree.Node): void => {
    const cur = this.current();
    if (cur && (path !== cur.path || chapterId !== cur.chapterId || cur.node !== node)) {
      cur.chapterId = chapterId;
      cur.path = path;
      cur.node = node;
    }
  };
  delete = (chapterId: string, path: Tree.Path, id: string) => {
    this.root.study!.makeChange('deleteComment', { ch: chapterId, path, id });
  };
}

export const viewDisabled = (root: AnalyseCtrl, why: string): VNode =>
  h('div.study__comments', [currentComments(root, true), h('div.study__message', why)]);

export function view(root: AnalyseCtrl): VNode {
  const study = root.study!,
    ctrl = study.commentForm,
    current = ctrl.current();
  if (!current) return viewDisabled(root, 'Select a move to comment');
  const setupTextarea = (vnode: VNode, old?: VNode) => {
    const el = vnode.elm as HTMLInputElement;
    const newKey = current.chapterId + current.path;

    if (old?.data!.path !== newKey) {
      const mine = (current.node.comments || []).find(function (c) {
        return isAuthorObj(c.by) && c.by.id && c.by.id === ctrl.root.opts.userId;
      });
      el.value = mine ? mine.text : '';
    }
    vnode.data!.path = newKey;

    if (ctrl.opening()) {
      requestAnimationFrame(() => el.focus());
      ctrl.opening(false);
    }
  };

  return h(
    'div.study__comments',
    { hook: onInsert(() => root.enableWiki(root.data.game.variant.key === 'standard')) },
    [
      currentComments(root, !study.members.canContribute()),
      h('form.form3', [
        h('textarea#comment-text.form-control', {
          hook: {
            insert(vnode) {
              setupTextarea(vnode);
              const el = vnode.elm as HTMLInputElement;
              el.oninput = () => setTimeout(() => ctrl.submit(el.value), 50);
              const heightStore = site.storage.make('study.comment.height');
              el.onmouseup = () => heightStore.set('' + el.offsetHeight);
              el.style.height = parseInt(heightStore.get() || '80') + 'px';

              $(el).on('keydown', e => {
                if (e.code === 'Escape') el.blur();
              });
            },
            postpatch: (old, vnode) => setupTextarea(vnode, old),
          },
        }),
      ]),
      h('div.analyse__wiki.study__wiki.force-ltr'),
    ],
  );
}
