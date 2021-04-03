import { h } from 'snabbdom';
import { VNode } from 'snabbdom';
import AnalyseCtrl from '../ctrl';
import { nodeFullName, bind, richHTML } from '../util';
import { StudyCtrl } from './interfaces';

function authorDom(author) {
  if (!author) return 'Unknown';
  if (!author.name) return author;
  return h(
    'span.user-link.ulpt',
    {
      attrs: { 'data-href': '/@/' + author.id },
    },
    author.name
  );
}

export function authorText(author): string {
  if (!author) return 'Unknown';
  if (!author.name) return author;
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
      const by: any = comment.by;
      const isMine = by.id && ctrl.opts.userId === by.id;
      if (!includingMine && isMine) return;
      const canDelete = isMine || study.members.isOwner();
      return h('div.study__comment.' + comment.id, [
        canDelete && study.vm.mode.write
          ? h('a.edit', {
              attrs: {
                'data-icon': 'q',
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
        isMine && study.vm.mode.write
          ? h('a.edit', {
              attrs: {
                'data-icon': 'm',
                title: 'Edit',
              },
              hook: bind(
                'click',
                _ => {
                  study.commentForm.start(chapter.id, ctrl.path, node);
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
