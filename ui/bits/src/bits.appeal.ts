import { formToXhr } from 'lib/xhr';

export function initModule(): void {
  if ($('.nav-tree').length) location.hash = location.hash || '#help-root';
  $('select.appeal-presets').on('change', function (this: HTMLSelectElement, e: Event) {
    $(this)
      .parents('form')
      .find('textarea')
      .val((e.target as HTMLTextAreaElement).value);
  });

  $('form.appeal__actions__slack').on('submit', (e: Event) => {
    const form = e.target as HTMLFormElement;
    formToXhr(form);
    $(form).find('button').text('Sent!').attr('disabled', 'true');
    return false;
  });

  const isoDateRegex = /\b(\d{4}-\d{2}-\d{2})\b/g;
  const now = new Date();
  now.setHours(0, 0, 0, 0);

  $('.appeal__msg--mod .appeal__msg__text').each(function (this: HTMLElement) {
    const walker = document.createTreeWalker(this, NodeFilter.SHOW_TEXT);

    let node: Text | null;
    while ((node = walker.nextNode() as Text | null)) {
      if (!node?.nodeValue) continue;
      const text = node.nodeValue;
      const matches = [...text.matchAll(isoDateRegex)];
      if (matches.length === 0) continue;

      const frag = document.createDocumentFragment();
      let lastIndex = 0;

      matches.forEach(match => {
        const date = new Date(match[0]);
        const color = date > now ? 'var(--c-bad)' : 'var(--c-good)';

        frag.append(text.slice(lastIndex, match.index));

        const span = document.createElement('span');
        span.style.color = color;
        span.textContent = date.toLocaleDateString(undefined, {
          year: 'numeric',
          month: 'long',
          day: 'numeric',
        });

        frag.append(span);
        lastIndex = match.index + match[0].length;
      });

      frag.append(text.slice(lastIndex));
      node.replaceWith(frag);
    }
  });
}
