export function setMode(textarea: HTMLElement, mode: 'write' | 'preview' = 'write'): void {
  const wrapper = textarea.closest<HTMLElement>('.markdown-textarea');

  if (mode === 'write') wrapper?.querySelector<HTMLElement>('.write-tab')?.click();
  else wrapper?.querySelector<HTMLElement>('.preview-tab')?.click();
}
