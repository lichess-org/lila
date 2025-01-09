export const userModInfo = (username: string): Promise<any> =>
  window.lishogi.xhr.json('GET', '/mod/chat-user/' + username);

export const flag = (resource: string, username: string, text: string): Promise<any> =>
  window.lishogi.xhr.json('POST', '/report/flag', {
    formData: { username, resource, text },
  });

export const getNote = (id: string): Promise<any> => window.lishogi.xhr.text('GET', noteUrl(id));

export const setNote = (id: string, text: string): Promise<any> =>
  window.lishogi.xhr.json('POST', noteUrl(id), {
    formData: { text },
  });

const noteUrl = (id: string) => `/${id}/note`;
