import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import AnalyseCtrl from '../ctrl';
import { nodeFullName, autolink, bind, innerHTML } from '../util';
import { StudyCtrl } from './interfaces';

function authorDom(author) {
  if (!author) return 'Unknown';
  if (!author.name) return author;
  return h('span.user_link.ulpt', {
    attrs: { 'data-href': '/@/' + author.id }
  }, author.name);
}

export function authorText(author): string {
  if (!author) return 'Unknown';
  if (!author.name) return author;
  return author.name;
}

const commentYoutubeRegex = /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/(?:.*?(?:[?&]v=)|v\/)|youtu\.be\/)(?:[^"&?\/ ]{11})\b/i;

export function enrichText(text: string, allowNewlines: boolean): string {
  let html = autolink(window.lichess.escapeHtml(text), url => {
    if (commentYoutubeRegex.test(url)) {
      const embedUrl = window.lichess.toYouTubeEmbedUrl(url);
      if (!embedUrl) return url;
      return '<iframe width="100%" height="300" src="' + embedUrl + '" frameborder=0 allowfullscreen></iframe>';
    }
    const show = url.replace(/https?:\/\//, '');
    return '<a target="_blank" rel="nofollow" href="' + url + '">' + show + '</a>';
  });
  if (allowNewlines) html = html.replace(/\n/g, '<br>');
  return html;
}

export function currentComments(ctrl: AnalyseCtrl, includingMine: boolean): VNode | undefined {
  if (!ctrl.node.comments) return;
  const node = ctrl.node,
  study: StudyCtrl = ctrl.study!,
  chapter = study.currentChapter(),
  comments = node.comments!;
  if (!comments.length) return;
  return h('div.study_comments', comments.map((comment: Tree.Comment) => {
    const by: any = comment.by;
    const isMine = by.id && ctrl.opts.userId === by.id;
    if (!includingMine && isMine) return;
    const canDelete = isMine || study.members.isOwner();
    return h('div.comment.' + comment.id, [
      canDelete ? h('a.edit', {
        attrs: {
          'data-icon': 'q',
          title: 'Delete'
        },
        hook: bind('click', _ => {
          if (confirm('Delete ' + authorText(by) + '\'s comment?'))
          study.commentForm.delete(chapter.id, ctrl.path, comment.id);
        }, ctrl.redraw)
      }) : null,
      isMine ? h('a.edit', {
        attrs: {
          'data-icon': 'm',
          title: 'Edit'
        },
        hook: bind('click', _ => {
          study.commentForm.open(chapter.id, ctrl.path, node);
        }, ctrl.redraw)
      }) : null,
      authorDom(by),
      ...(node.san ? [
        ' on ',
        h('span.node', nodeFullName(node))
      ] : []),
      ': ',
      h('div.text', {
        hook: innerHTML(comment.text, text => enrichText(text, true))
      })
    ]);
  }));
}
