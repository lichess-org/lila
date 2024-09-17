

export function initModule({ danger }: { danger: boolean }): void {
  setTimeout(() => {
    const el = document.getElementById('oauth-authorize')!;
    el.removeAttribute('disabled');
    el.className = 'button';
    if (danger) el.classList.add('button-red', 'confirm', 'text');
  }, danger ? 5000 : 2000);
}