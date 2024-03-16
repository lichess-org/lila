import * as xhr from 'common/xhr';
import debounce from 'debounce-promise';
import { initAll as initMiniBoards } from 'common/miniBoard';

export function init() {
  const debounced = debounce((str: string) => {
    const q = str.trim();
    if (q)
      xhr.text(xhr.url('/opening', { q })).then((html: string) => {
        selectResults().replaceWith(html).removeClass('none');
        initMiniBoards();
      });
    else {
      selectResults().addClass('none');
      Promise.resolve();
    }
  }, 150);
  $('.opening__search-form__input').on('input', e => {
    debounced((e.target as HTMLInputElement).value);
  });
}

const selectResults = () => $('.opening__search__results');
