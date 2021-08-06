import Tagify from '@yaireo/tagify';
import debounce from 'debounce-promise';
import * as xhr from 'common/xhr';

export function leadersStart() {
  const input = document.getElementById('form3-leaders') as HTMLInputElement;

  const tagify = new Tagify(input, {
    pattern: /.{3,}/,
    maxTags: 10,
    enforceWhitelist: true,
    whitelist: input.value.trim().split(/\s*,\s*/),
  });
  const doFetch: (term: string) => Promise<string[]> = debounce(
    (term: string) => xhr.json(xhr.url('/player/autocomplete', { term, names: 1 })),
    300
  );
  tagify.on('input', e => {
    const term = e.detail.value.trim();
    if (term.length < 3) return;
    tagify.settings.whitelist!.length = 0; // reset the whitelist
    // show loading animation and hide the suggestions dropdown
    tagify.loading(true).dropdown.hide.call(tagify);
    doFetch(term).then((list: string[]) => {
      tagify.settings.whitelist!.splice(0, list.length, ...list); // update whitelist Array in-place
      tagify.loading(false).dropdown.show.call(tagify, term); // render the suggestions dropdown
    });
  });
}

export function membersStart() {
  const studentsInput = document.getElementById('form3-students') as HTMLInputElement;
  const membersInput = document.getElementById('form3-members') as HTMLInputElement;
  const students: string[] = [];
  const members: string[] = [];

  function toggle(collection: string[], item: string) {
    const id = collection.indexOf(item);
    if (id !== -1) collection.splice(id, 1);
    else collection.push(item);
  }

  if (studentsInput) {
    $('.student').on('click', e => {
      $(e.target).toggleClass('button-empty button-green');
      toggle(students, e.target.value.trim());
      studentsInput.value = students.join(',');
    });
  }

  $('.member').on('click', e => {
    $(e.target).toggleClass('button-empty button-red');
    toggle(members, e.target.value.trim());
    membersInput.value = members.join(',');
  });
}
