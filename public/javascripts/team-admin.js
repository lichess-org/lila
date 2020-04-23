$(() => {
  const tagify = new Tagify(document.getElementById('form3-leaders'), {
    pattern: /.{3,}/,
    maxTags: 30
  });
  let abortCtrl; // for aborting the call
  tagify.on('input', e => {
    const term = e.detail.value.trim();
    if (term.length < 3) return;
    tagify.settings.whitelist.length = 0; // reset the whitelist
    abortCtrl && abortCtrl.abort();
    abortCtrl = new AbortController();
    // show loading animation and hide the suggestions dropdown
    tagify.loading(true).dropdown.hide.call(tagify);

    fetch(`/player/autocomplete?term=${encodeURIComponent(term)}&names=1`, {signal: abortCtrl.signal})
      .then(r => r.json())
      .then(list => {
        tagify.settings.whitelist.splice(0, list.length, ...list); // update whitelist Array in-place
        tagify.loading(false).dropdown.show.call(tagify, term); // render the suggestions dropdown
      })
  });
});
