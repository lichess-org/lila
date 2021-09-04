import { h, VNode } from 'snabbdom';
import { bind } from 'common/snabbdom';
import AnalyseCtrl from '../ctrl';
import { nodeFullName, richHTML } from '../util';
import { StudyCtrl } from './interfaces';

export type AuthorObj = {
  id: string;
  name: string;
};
export type Author = AuthorObj | string;

function authorDom(author: Author): string | VNode {
  if (!author) return 'Unknown';
  if (typeof author === 'string') return author;
  return h(
    'span.user-link.ulpt',
    {
      attrs: { 'data-href': '/@/' + author.id },
    },
    author.name
  );
}

export function isAuthorObj(author: Author): author is AuthorObj {
  return typeof author === 'object';
}

export function authorText(author?: Author): string {
  if (!author) return 'Unknown';
  if (typeof author === 'string') return author;
  return author.name;
}

export function currentComments(ctrl: AnalyseCtrl, includingMine: boolean): VNode | undefined {
  if (!ctrl.node.comments) return;
  const node = ctrl.node,
    study: StudyCtrl = ctrl.study!,
    chapter = study.currentChapter(),
    comments = node.comments!;
  if (!comments.length) return;
  return h(
    'div',
    comments.map((comment: Tree.Comment) => {
      const by: Author = comment.by;
      const isMine = isAuthorObj(by) && by.id === ctrl.opts.userId;
      if (!includingMine && isMine) return;
      const canDelete = isMine || study.members.isOwner();
      return h('div.study__comment.' + comment.id, [
        canDelete && study.vm.mode.write
          ? h('a.edit', {
              attrs: {
                'data-icon': '',
                title: 'Delete',
              },
              hook: bind(
                'click',
                _ => {
                  if (confirm('Delete ' + authorText(by) + "'s comment?"))
                    study.commentForm.delete(chapter.id, ctrl.path, comment.id);
                },
                ctrl.redraw
              ),
            })
          : null,
        authorDom(by),
        ...(node.san ? [' on ', h('span.node', nodeFullName(node))] : []),
        ': ',
        h('div.text', { hook: richHTML(comment.text) }),
      ]);
    })
  );
}
