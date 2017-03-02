var m = require('mithril');
var nodeFullName = require('../util').nodeFullName;
var renderComment = require('./studyComments').embedYoutube;

function authorDom(author) {
  if (!author) return 'Unknown';
  if (!author.name) return author;
  return m('span.user_link.ulpt', {
    'data-href': '/@/' + author.id
  }, author.name);
}

function authorText(author) {
  if (!author) return 'Unknown';
  if (!author.name) return author;
  return author.name;
}

var commentYoutubeRegex = /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/\S*/gi;

function embedYoutube(text, allowNewlines) {
  var html = lichess.escapeHtml(text).replace(commentYoutubeRegex, function(found) {
    if (found.indexOf('http://') !== 0 && found.indexOf('https://') !== 0) {
      found = 'https://' + found;
    }

    // https://www.abeautifulsite.net/parsing-urls-in-javascript
    var parser = document.createElement('a');
    parser.href = found;

    var queries = parser.search.replace(/^\?/, '').split('&');
    var videoId;

    for (var i = 0; i < queries.length; i++) {
        var split = queries[i].split('=');

        if (split.length >= 2 && split[0] === 'v') {
          videoId = split[1];
          break;
        }
    }

    if (!videoId) {
      var pathParts = parser.pathname.split('/');

      if (pathParts.length >= 2) {
        if (parser.hostname.indexOf('youtu.be') === 0) {
          videoId = pathParts[1];
        } else if (pathParts.length >= 3 && pathParts[1] === 'v') {
          videoId = pathParts[2];
        }
      }
    }

    if (!videoId || videoId.length !== 11) return found;
    var url = lichess.toYouTubeEmbedUrl('https://youtube.com/watch?v=' + videoId);
    if (!url) return found;
    return '<iframe width="100%" height="300" src="' + url + '" frameborder=0 allowfullscreen></iframe>';
  });
  if (allowNewlines) html = html.replace(/\n/g, '<br>');
  return m.trust(html);
}

module.exports = {
  authorText: authorText,
  embedYoutube: embedYoutube,
  currentComments: function(ctrl, includingMine) {
    var path = ctrl.vm.path;
    var node = ctrl.vm.node;
    var chapter = ctrl.study.currentChapter();
    var comments = node.comments || [];
    if (!comments.length) return;
    return m('div.study_comments', comments.map(function(comment) {
      var isMine = comment.by && comment.by.id && ctrl.userId === comment.by.id;
      if (!includingMine && isMine) return;
      var canDelete = isMine || ctrl.study.members.isOwner();
      return m('div.comment', [
        canDelete ? m('a.edit[data-icon=q][title=Delete]', {
          onclick: function() {
            if (confirm('Delete ' + authorText(comment.by) + '\'s comment?'))
              ctrl.study.commentForm.delete(chapter.id, path, comment.id);
          }
        }) : null,
        isMine ? m('a.edit[data-icon=m][title=Edit]', {
          onclick: function() {
            ctrl.study.commentForm.open(chapter.id, path, node);
          }
        }) : null,
        authorDom(comment.by),
        node.san ? [
          ' on ',
          m('span.node', nodeFullName(node))
        ] : null,
        ': ',
        m('div.text', embedYoutube(comment.text))
      ]);
    }));
  }
};
