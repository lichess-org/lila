import * as xhr from 'lib/xhr';
import { alert, prompt } from 'lib/view/dialogs';
import { throttlePromiseDelay } from 'lib/async';

site.load.then(() => {
  $('.flash').addClass('fade');
  $('.ublog-post__like').on(
    'click',
    throttlePromiseDelay(
      () => 1000,
      async function (this: HTMLButtonElement) {
        const button = $(this),
          likeClass = 'ublog-post__like--liked',
          liked = !button.hasClass(likeClass);
        return await xhr
          .text(`/ublog/${button.data('rel')}/like?v=${liked}`, {
            method: 'post',
          })
          .then(likes => {
            const label = $('.ublog-post__like .button-label');
            const newText = label.data(`i18n-${liked ? 'unlike' : 'like'}`);
            label.text(newText);
            $('.ublog-post__like').toggleClass(likeClass, liked).attr('title', newText);
            $('.ublog-post__like__nb').text(likes);
          });
      },
    ),
  );
  $('.ublog-post__follow button').on(
    'click',
    throttlePromiseDelay(
      () => 1000,
      async function (this: HTMLButtonElement) {
        const button = $(this),
          followClass = 'followed';
        return await xhr
          .text(button.data('rel'), {
            method: 'post',
          })
          .then(() => button.parent().toggleClass(followClass));
      },
    ),
  );
  $('#form3-tier').on('change', function (this: HTMLSelectElement) {
    (this.parentNode as HTMLFormElement).submit();
  });
  rewireModTools();
});

type SubmitForm = {
  quality?: number;
  evergreen?: boolean;
  flagged?: string;
  commercial?: string;
  featured?: boolean;
  featuredUntil?: number;
};

function rewireModTools() {
  const modToolsContainer = document.querySelector<HTMLElement>('#mod-tools-container');
  if (!modToolsContainer?.firstElementChild) return;
  const modTools = modToolsContainer.firstElementChild as HTMLElement;
  const submitBtn = modTools.querySelector<HTMLButtonElement>('.submit')!;
  const submit = async (o: SubmitForm) => {
    const rsp = await xhr.textRaw(modTools.dataset.url!, {
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
      body: JSON.stringify(o),
    });
    if (!rsp.ok) return alert(`Error ${rsp.status}: ${rsp.statusText}`);
    modToolsContainer.innerHTML = await rsp.text();
    rewireModTools();
  };

  modTools
    .querySelectorAll<HTMLButtonElement>('.quality-btn')
    .forEach(btn => btn.addEventListener('click', () => submit({ quality: Number(btn.value) })));

  const submitFields = modTools.querySelector<HTMLElement>('.submit-fields')!;
  submitFields.querySelectorAll<HTMLInputElement>('input').forEach(input =>
    input.addEventListener('input', () => {
      input.parentElement!.classList.toggle('empty', !input.value.trim());
      submitBtn.classList.remove('none');
      submitBtn.disabled = false;
    }),
  );
  submitBtn.addEventListener('click', () => {
    const form: Record<string, any> = {};
    for (const input of submitFields.querySelectorAll<HTMLInputElement>('input')) {
      form[input.id] = input.type === 'checkbox' ? input.checked : input.value;
    }
    submit(form);
  });
  modTools
    .querySelector<HTMLElement>('.carousel-add-btn')
    ?.addEventListener('click', () => submit({ featured: true }));
  modTools
    .querySelector<HTMLElement>('.carousel-remove-btn')
    ?.addEventListener('click', () => submit({ featured: false }));
  modTools.querySelector<HTMLElement>('.carousel-pin-btn')?.addEventListener('click', async () => {
    const days = await prompt('How many days?', '7', (n: string) => Number(n) > 0 && Number(n) < 31);
    if (days) submit({ featured: true, featuredUntil: Number(days) });
  });
}
