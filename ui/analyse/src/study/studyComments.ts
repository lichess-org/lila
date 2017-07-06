import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import AnalyseController from '../ctrl';
import { nodeFullName, autolink, bind } from '../util';

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

const commentYoutubeRegex = /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com\/(?:.*?(?:[?&]v=)|v\/)|youtu\.be\/)(?:[^"&?\/ ]{11})\b/gi;

export function enrichText(text: string, allowNewlines: boolean): string {
  var html = autolink(window.lichess.escapeHtml(text), url => {
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

export function currentComments(ctrl: AnalyseController, includingMine: boolean): VNode | undefined {
  const node = ctrl.node;
  const chapter = ctrl.study.currentChapter();
  const comments = node.comments || [];
  if (!comments.length) return;
  return h('div.study_comments', comments.map(function(comment) {
    const isMine = comment.by && (comment.by as any).id && ctrl.opts.userId === (comment.by as any).id;
    if (!includingMine && isMine) return;
    const canDelete = isMine || ctrl.study.members.isOwner();
    return h('div.comment', [
      canDelete ? h('a.edit', {
        attrs: {
          'data-icon': 'q',
          title: 'Delete'
        },
        hook: bind('click', _ => {
          if (confirm('Delete ' + authorText(comment.by) + '\'s comment?'))
          ctrl.study!.commentForm.delete(chapter.id, ctrl.path, comment.id);
        }, ctrl.redraw)
      }) : null,
      isMine ? h('a.edit', {
        attrs: {
          'data-icon': 'm',
          title: 'Edit'
        },
        hook: bind('click', _ => {
          ctrl.study.commentForm.open(chapter.id, ctrl.path, node);
        }, ctrl.redraw)
      }) : null,
      authorDom(comment.by),
      ...(node.san ? [
        ' on ',
        h('span.node', nodeFullName(node))
      ] : []),
      ': ',
      h('div.text', enrichText(comment.text, false))
    ]);
  }));
}
