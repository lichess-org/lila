import debounce from 'debounce-promise';
import * as xhr from 'common/xhr';
import Tagify from '@yaireo/tagify';

lichess.load.then(() => {
  const tagify = new Tagify(document.getElementById('form3-topics') as HTMLInputElement, {
    pattern: /.{2,}/,
    maxTags: 30,
  });
  const doFetch: (term: string) => Promise<string[]> = debounce(
    (term: string) => xhr.json(xhr.url('/study/topic/autocomplete', { term })),
    300
  );
  tagify.on('input', e => {
    const term = e.detail.value.trim();
    if (term.length < 2) return;
    tagify.settings.whitelist!.length = 0; // reset the whitelist
    // show loading animation and hide the suggestions dropdown
    tagify.loading(true).dropdown.hide.call(tagify);
    doFetch(term).then((list: string[]) => {
      tagify.settings.whitelist!.splice(0, list.length, ...list); // update whitelist Array in-place
      tagify.loading(false).dropdown.show.call(tagify, term); // render the suggestions dropdown
    });
  });
});
