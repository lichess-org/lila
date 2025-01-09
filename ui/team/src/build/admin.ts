import Tagify from '@yaireo/tagify';
import debounce from 'debounce-promise';

window.lishogi.ready.then(() => {
  const input = document.getElementById('form3-leaders') as HTMLInputElement;
  const tagify = new Tagify(input, {
    pattern: /.{3,}/,
    maxTags: 30,
    whitelist: input.value.trim().split(/\s*,\s*/),
    hooks: {
      beforePaste: (_, data) => {
        data.tagify.settings.enforceWhitelist = false;
        return Promise.resolve(undefined);
      },
    },
  });
  const doFetch: (term: string) => Promise<string[]> = debounce(
    (term: string) =>
      window.lishogi.xhr.json('GET', '/api/player/autocomplete', { url: { term, names: true } }),
    300
  );
  tagify.on('input', e => {
    const term = (e.detail as Tagify.TagData).value.trim();
    if (term.length < 3) return;
    tagify.whitelist = [];
    tagify.settings.enforceWhitelist = true;
    // show loading animation and hide the suggestions dropdown
    tagify.loading(true).dropdown.hide.call(tagify);
    doFetch(term).then((list: string[]) => {
      tagify.whitelist = list;
      tagify.loading(false).dropdown.show.call(tagify, term); // render the suggestions dropdown
    });
  });
});
