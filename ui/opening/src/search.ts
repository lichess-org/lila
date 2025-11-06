import { text as xhrText, url as xhrUrl } from 'lib/xhr';
import debounce from 'debounce-promise';
import { initMiniBoards } from 'lib/view';

export function init(): void {
  const debounced = debounce((str: string) => {
    const q = str.trim();
    if (q)
      xhrText(xhrUrl('/opening', { q })).then((html: string) => {
        selectResults().replaceWith(html).removeClass('none');
        initMiniBoards();
      });
    else {
      selectResults().addClass('none');
    }
  }, 150);
  $('.opening__search-form__input').on('input', e => {
    debounced((e.target as HTMLInputElement).value);
  });
}

const selectResults = () => $('.opening__search__results');
