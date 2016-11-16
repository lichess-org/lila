var headers = {
  'Accept': 'application/vnd.lichess.v2+json'
};

function userModInfo(username, then) {
  $.ajax({
    url: '/mod/chat-user/' + username,
    headers: headers,
    success: then
  });
}

function noteUrl(id) {
  return '/' + id + '/note';
}

function getNote(id) {
  return $.get(noteUrl(id));
}

function setNote(id, text) {
  $.ajax({
    url: noteUrl(id),
    method: 'post',
    data: {
      text: text
    },
    headers: headers
  });
}

module.exports = {
  userModInfo: userModInfo,
  getNote: getNote,
  setNote: setNote
};
