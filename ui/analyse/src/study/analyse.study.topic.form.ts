import debounce from 'debounce-promise';
import { json as xhrJson, url as xhrUrl } from 'lib/xhr';
import Tagify from '@yaireo/tagify';

site.load.then(() => {
  const input = document.getElementById('form3-topics') as HTMLInputElement;
  const tagify = new Tagify(input, {
    pattern: /.{2,}/,
    maxTags: parseInt(input?.dataset['max'] ?? '64'),
  });
  const doFetch: (term: string) => Promise<string[]> = debounce(
    (term: string) => xhrJson(xhrUrl('/study/topic/autocomplete', { term })),
    300,
  );
  let clickDebounce: Timeout | undefined; // https://yaireo.github.io/tagify/#section-advance-options
  tagify
    .on('input', e => {
      const term = (e.detail as Tagify.BaseTagData).value.trim();
      if (term.length < 2) return;
      tagify.settings.whitelist.length = 0; // reset the whitelist
      // show loading animation and hide the suggestions dropdown
      tagify.loading(true).dropdown.hide.call(tagify);
      doFetch(term).then((list: string[]) => {
        tagify.settings.whitelist.splice(0, list.length, ...list); // update whitelist Array in-place
        tagify.loading(false).dropdown.show.call(tagify, term); // render the suggestions dropdown
      });
    })
    .on('click', e => {
      clearTimeout(clickDebounce);
      clickDebounce = setTimeout(() => {
        if (!e.detail.tag.classList.contains('tagify__tag--editable'))
          location.href = `/study/topic/${encodeURIComponent(e.detail.data.value)}/mine`;
      }, 200);
    })
    .on('dblclick', _ => clearTimeout(clickDebounce));
});
