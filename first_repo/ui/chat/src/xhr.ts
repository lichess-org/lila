import { json, text, form } from 'common/xhr';

export const userModInfo = (username: string) => json('/mod/chat-user/' + username);

export const flag = (resource: string, username: string, text: string) =>
  json('/report/flag', {
    method: 'post',
    body: form({ username, resource, text }),
  });

export const getNote = (id: string) => text(noteUrl(id));

export const setNote = (id: string, text: string) =>
  json(noteUrl(id), {
    method: 'post',
    body: form({ text }),
  });

const noteUrl = (id: string) => `/${id}/note`;
