import * as xhr from 'lib/xhr';
import { alert, prompt } from 'lib/view/dialogs';
import { throttlePromiseDelay } from 'lib/async';
import { domDialog } from 'lib/view/dialog';
import { escapeHtml } from 'lib';
import { spinnerHtml } from 'lib/view/controls';

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
  const tierEl = document.querySelector<HTMLSelectElement>('#form3-tier');
  modBlogOrigTier = tierEl?.value ?? '';

  tierEl?.addEventListener('change', e => showModBlogSubmitDlg(e));
  console.log(tierEl, document.querySelector('.ublog-mod-note-btn'));
  document.querySelector<HTMLElement>('.ublog-mod-note-btn')?.addEventListener('click', showModBlogSubmitDlg);
  rewireModPost();
});

let modBlogOrigTier: string;

async function showModBlogSubmitDlg(e: Event) {
  const form = document.querySelector<HTMLFormElement>('.ublog-mod-blog-form');
  if (!form) return;
  e.preventDefault();
  const noteField = form.querySelector<HTMLInputElement>('[name="note"]')!;
  const noteHtml = escapeHtml(noteField.value.trim());
  const res = await domDialog({
    class: 'ublog-mod-note-dlg',
    modal: true,
    show: true,
    actions: [
      { selector: '.cancel', result: 'cancel' },
      {
        selector: '.submit',
        listener: (_, dlg) => {
          const textArea = dlg.view.querySelector<HTMLTextAreaElement>('.note')!;
          noteField.value = textArea.value.trim();
          dlg.close();
          form.submit();
        },
      },
    ],
    htmlText: $html`
      <textarea class="note" rows="5" cols="50" placeholder="Mod notes" maxlength="800">${noteHtml}</textarea>
      <span>
        <button class="button button-empty button-red cancel">cancel</button>
        <button class="button button-metal submit">submit</button>
      </span>`,
  });
  if (res.returnValue === 'cancel')
    form.querySelector<HTMLSelectElement>('#form3-tier')!.value = modBlogOrigTier;
}

type SubmitForm = {
  quality?: number;
  evergreen?: boolean;
  flagged?: string;
  commercial?: string;
  featured?: boolean;
  featuredUntil?: number;
};

function rewireModPost() {
  const modToolsContainer = document.querySelector<HTMLElement>('#mod-tools-container');
  if (!modToolsContainer?.firstElementChild) return;
  const modTools = modToolsContainer.firstElementChild as HTMLElement;
  const submitBtn = modTools.querySelector<HTMLButtonElement>('.submit')!;
  const assessBtn = modTools.querySelector<HTMLButtonElement>('.assess-btn')!;
  const submit = async (o: SubmitForm) => {
    const rsp = await xhr.textRaw(modTools.dataset.url!, {
      headers: { 'Content-Type': 'application/json' },
      method: 'POST',
      body: JSON.stringify(o),
    });
    if (!rsp.ok) return alert(`Error ${rsp.status}: ${rsp.statusText}`);
    modToolsContainer.innerHTML = await rsp.text();
    rewireModPost();
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
  assessBtn.addEventListener('click', async () => {
    assessBtn.insertAdjacentHTML('afterend', spinnerHtml);
    assessBtn.disabled = true;
    assessBtn.classList.add('disabled');
    const rsp = await xhr.textRaw(assessBtn.dataset.url!, { method: 'POST' });
    if (!rsp.ok) return alert(`Error ${rsp.status}: ${rsp.statusText}`);
    modToolsContainer.innerHTML = await rsp.text();
    rewireModPost();
  });
}
