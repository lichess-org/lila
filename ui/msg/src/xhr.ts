const headers = {
  'Accept': 'application/vnd.lichess.v4+json'
};

export function loadThread(userId: string) {
  return $.ajax({
    url: `/inbox/${userId}`,
    headers,
    cache: false
  });
}
