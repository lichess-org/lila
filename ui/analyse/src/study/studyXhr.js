var headers = {
  'Accept': 'application/vnd.lichess.v2+json'
};

module.exports = {

  reload: function(baseUrl, id, chapterId) {
    var url = '/' + baseUrl + '/' + id;
    if (chapterId) url += '/' + chapterId;
    return $.ajax({
      url: url,
      headers: headers
    });
  },

  variants: function() {
    return $.ajax({
      url: '/variant',
      headers: headers,
      cache: true
    });
  },

  glyphs: function() {
    return $.ajax({
      url: '/glyphs',
      headers: headers,
      cache: true
    });
  },

  chapterConfig: function(studyId, chapterId) {
    return $.ajax({
      url: ['/study', studyId, chapterId, 'meta'].join('/'),
      headers: headers
    });
  },

  practiceComplete: function(chapterId, nbMoves) {
    return $.ajax({
      method: 'POST',
      url: ['/practice/complete', chapterId, nbMoves].join('/'),
      headers: headers
    });
  }
};
