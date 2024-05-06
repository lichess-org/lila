import Tagify from '@yaireo/tagify';
import debounce from 'debounce-promise';
import * as xhr from 'common/xhr';

site.load.then(() => {
  $('#form3-leaders').each(function (this: HTMLInputElement) {
    initTagify(this, 10);
  });
  $('#form3-members').each(function (this: HTMLInputElement) {
    initTagify(this, 100);
  });
  $('form.team-add-leader input[name="name"]').each(function (this: HTMLInputElement) {
    site.asset.userComplete({
      input: this,
      team: this.dataset.teamId,
      tag: 'span',
    });
  });
  $('form.team-permissions table').each(function (this: HTMLTableElement) {
    permissionsTable(this);
  });
  $('form.team-declined-request input[name="search"]').each(function (this: HTMLInputElement) {
    site.asset.userComplete({
      input: this,
      tag: 'span',
    });
  });
});

function permissionsTable(table: HTMLTableElement) {
  $(table)
    .find('tbody td')
    .on('mouseenter', function (this: HTMLTableCellElement) {
      const index = $(this).index() + 1;
      $(table).find('.highlight').removeClass('highlight');
      $(table).find(`tbody td:nth-child(${index}), thead th:nth-child(${index})`).addClass('highlight');
    });
  $(table).on('mouseleave', function () {
    $(table).find('.highlight').removeClass('highlight');
  });
}

function initTagify(input: HTMLInputElement, maxTags: number) {
  const team = input.dataset.rel;
  const tagify = new Tagify(input, {
    pattern: /.{3,}/,
    maxTags,
    whitelist: [],
    hooks: {
      beforePaste: (_, data) => {
        data.tagify.settings.enforceWhitelist = false;
        return Promise.resolve(undefined);
      },
    },
  });
  const doFetch: (term: string) => Promise<string[]> = debounce(
    (term: string) => xhr.json(xhr.url('/api/player/autocomplete', { term, names: 1, team })),
    300,
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
}
