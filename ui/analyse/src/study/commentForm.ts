import { prop } from 'lib';
import { onInsert } from 'lib/view';
import { throttle } from 'lib/async';
import { h, type VNode } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import { currentComments, isAuthorObj } from './studyComments';
import { storage } from 'lib/storage';
import type { TreeNode, TreePath } from 'lib/tree/types';
import type { ChapterId } from './interfaces';

interface Current {
  chapterId: ChapterId;
  path: TreePath;
  node: TreeNode;
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

  start = (chapterId: string, path: TreePath, node: TreeNode): void => {
    this.opening(true);
    this.current({ chapterId, path, node });
    this.root.userJump(path);
  };

  onSetPath = (chapterId: string, path: TreePath, node: TreeNode): void => {
    const cur = this.current();
    if (cur && (path !== cur.path || chapterId !== cur.chapterId || cur.node !== node)) {
      cur.chapterId = chapterId;
      cur.path = path;
      cur.node = node;
    }
  };
  delete = (chapterId: string, path: TreePath, id: string) => {
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
      const mine = (current.node.comments || []).find(
        c => isAuthorObj(c.by) && c.by.id && c.by.id === ctrl.root.opts.userId,
      );
      el.value = mine?.text || '';
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
              const heightStore = storage.make('study.comment.height');
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
