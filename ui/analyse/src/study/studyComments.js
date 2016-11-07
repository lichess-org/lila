var m = require('mithril');
var nodeFullName = require('../util').nodeFullName;

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

module.exports = {
  authorText: authorText,
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
        m('span.text', comment.text)
      ]);
    }));
  }
};
