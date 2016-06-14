var m = require('mithril');

var xhrConfig = function(xhr) {
  xhr.setRequestHeader('X-Requested-With', 'XMLHttpRequest');
  xhr.setRequestHeader('Accept', 'application/vnd.lichess.v1+json');
};

function uncache(url) {
  return url + '?_=' + new Date().getTime();
}

function userModInfo(username) {
  return m.request({
    background: true,
    method: 'GET',
    url: uncache('/mod/chat-user/' + username),
    config: xhrConfig
  });
}

function noteUrl(id) {
  return uncache('/' + id + '/note');
}

function getNote(id) {
  return $.get(noteUrl(id));
}

function setNote(id, text) {
  return m.request({
    background: true,
    method: 'POST',
    url: noteUrl(id),
    data: {
      text: text
    },
    config: xhrConfig
  });
}

module.exports = {
  userModInfo: userModInfo,
  getNote: getNote,
  setNote: setNote
};
