export function load() {
  return $.get('/challenge');
}
export function decline(id: string) {
  return $.post(`/challenge/${id}/decline`);
}
export function cancel(id: string) {
  return $.post(`/challenge/${id}/cancel`);
}
