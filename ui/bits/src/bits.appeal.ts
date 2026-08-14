import { formToXhr } from 'lib/xhr';

export function initModule(): void {
  if ($('.nav-tree').length) location.hash = location.hash || '#help-root';

  $('.appeal-presets button').on('click', function (this: HTMLSelectElement, e: Event) {
    $(this)
      .parents('form')
      .find('#form3-text')
      .val((e.target as HTMLButtonElement).value);
  });

  $('form.appeal__actions__zulip').on('submit', (e: SubmitEvent) => {
    const form = e.target as HTMLFormElement;
    formToXhr(form);
    $(form).find('button').text('Sent!').attr('disabled', 'true');
    return false;
  });

  $('form select[name="months"]').on('click', function (this: HTMLSelectElement) {
    if (this.value) (this.parentElement as HTMLFormElement).submit();
  });

  initInfoStep();
  initAccountsStep();
}

function initInfoStep(): void {
  document.querySelectorAll<HTMLElement>('.appeal-info').forEach(root => {
    const checkbox = root.querySelector<HTMLInputElement>('.appeal-info__agree input[type="checkbox"]');
    const continueButton = root.querySelector<HTMLAnchorElement>('.appeal-info__continue');
    if (!checkbox || !continueButton) return;

    const sync = () => continueButton.classList.toggle('disabled', !checkbox.checked);
    checkbox.addEventListener('change', sync);
    continueButton.addEventListener('click', e => {
      if (!checkbox.checked) e.preventDefault();
    });
    sync();
  });
}

function initAccountsStep(): void {
  document.querySelectorAll<HTMLElement>('.appeal-accounts').forEach(root => {
    const leafId = root.dataset.leaf;
    const onlyRadio = root.querySelector<HTMLInputElement>('.appeal-accounts__only');
    const othersRadio = root.querySelector<HTMLInputElement>('.appeal-accounts__others-radio');
    const othersText = root.querySelector<HTMLTextAreaElement>('textarea.appeal-accounts__others');
    const forgotten = root.querySelector<HTMLInputElement>(
      '.appeal-accounts__forgotten input[type="checkbox"]',
    );
    const household = root.querySelector<HTMLTextAreaElement>('.appeal-accounts__household');
    const continueButton = root.querySelector<HTMLAnchorElement>('.appeal-accounts__continue');
    if (!leafId || !onlyRadio || !othersRadio || !othersText || !forgotten || !household || !continueButton)
      return;

    const canContinue = () =>
      onlyRadio.checked || (othersRadio.checked && (othersText.value.trim().length > 0 || forgotten.checked));

    const sync = () => {
      const others = othersRadio.checked;
      othersText.disabled = !others;
      forgotten.disabled = !others;
      if (!others) {
        othersText.value = '';
        forgotten.checked = false;
      }
      continueButton.classList.toggle('disabled', !canContinue());
    };

    const copyToAppealForm = (): boolean => {
      if (!canContinue()) return false;
      const form = document.querySelector<HTMLFormElement>(`#help-${leafId} form`);
      if (!form) return false;
      const set = (name: string, value: string) => {
        let input = form.querySelector<HTMLInputElement>(`input[name="${name}"]`);
        if (!input) {
          input = document.createElement('input');
          input.type = 'hidden';
          input.name = name;
          form.appendChild(input);
        }
        input.value = value;
      };
      set('accounts.otherUsernames', othersRadio.checked ? othersText.value : '');
      set('accounts.moreForgotten', forgotten.checked ? 'true' : 'false');
      set('accounts.household', household.value);
      return true;
    };

    onlyRadio.addEventListener('change', sync);
    othersRadio.addEventListener('change', sync);
    othersText.addEventListener('input', sync);
    forgotten.addEventListener('change', sync);
    continueButton.addEventListener('click', e => {
      if (!copyToAppealForm()) e.preventDefault();
    });
    sync();
  });
}
