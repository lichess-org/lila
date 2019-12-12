export function userModInfo(username: string) {
  return $.get('/mod/chat-user/' + username)
}

export function flag(resource: string, username: string, text: string) {
  return $.post('/report/flag', { username, resource, text });
}

export function getNote(id: string) {
  return $.get(noteUrl(id));
}

export function setNote(id: string, text: string) {
  return $.post(noteUrl(id), { text })
}

function noteUrl(id: string) {
  return `/${id}/note`;
}
