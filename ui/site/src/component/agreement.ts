import { formToXhr } from 'common/xhr';

export default function () {
  $('#agreement form').on('submit', (e: Event) => {
    const form = e.target as HTMLFormElement;
    formToXhr(form);
    (form.parentNode as HTMLDivElement).remove();
    return false;
  });
}
