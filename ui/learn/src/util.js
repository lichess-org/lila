module.exports = {
  toLesson: function(l, it) {
    l.id = it + 1;
    return l;
  },
  toStage: function(s, it) {
    s.id = it + 1;
    s.color = / w /.test(s.fen) ? 'white' : 'black';
    return s;
  },
  assetUrl: $('body').data('asset-url') + '/assets/'
};
