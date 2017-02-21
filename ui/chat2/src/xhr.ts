export function userModInfo(username: string) {
  return $.get('/mod/chat-user/' + username)
}

export function getNote(id: string) {
  return $.get(noteUrl(id));
}

export function setNote(id: string, text: string) {
  console.log(id, text)
  return $.post(noteUrl(id), { text: text })
}

function noteUrl(id: string) {
  return '/' + id + '/note';
}
