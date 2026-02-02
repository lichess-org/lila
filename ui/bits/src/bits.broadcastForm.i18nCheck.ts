import { Checkmark } from 'lib/licon';
export default function initModule(): void {
  site.load.then(() => {
    const nameInput = document.querySelector<HTMLInputElement>('#form3-name');
    if (!nameInput) return;
    const isRoundEdit = location.pathname.startsWith('/broadcast/round/');
    const groupName = document.querySelector('.relay-form__subnav__group')?.textContent;

    // Must be in sync with file://./../../../modules/relay/src/main/RelayI18n.scala
    const regexes = [
      /^Round (\d+)$/i,
      /^Game (\d+)$/i,
      /^blitz$/i,
      /^rapid$/i,
      /^classical$/i,
      /^open$/i,
      /^women$/i,
      /^girls$/i,
      /^open\s+u(\d+)$/i,
      /^girls\s+u(\d+)$/i,
      /^quarter[-\s]?final[s]?$/i,
      /^semi[-\s]?final[s]?$/i,
      /^final[s]?$/i,
      /^tie[-\s]?break(?:er)?[s]?$/i,
      /^knock[-\s]?out[s]?$/i,
    ];
    const helpEl = document.createElement('small');
    helpEl.classList.add('form-help');
    helpEl.innerHTML = `${i18n.broadcast.automaticallyTranslated}<br>`;
    helpEl.setAttribute('data-icon', Checkmark);
    helpEl.style.visibility = 'hidden';
    nameInput.insertAdjacentElement('afterend', helpEl);
    const checkAndToggle = (name: string) => {
      const isTranslatable =
        !!name && name.split(/\s+\|\s+/).every(token => regexes.some(re => re.test(token)));
      isTranslatable ? (helpEl.style.visibility = 'visible') : (helpEl.style.visibility = 'hidden');
    };
    const getShortName = () =>
      isRoundEdit
        ? nameInput.value
        : groupName
          ? nameInput.value.startsWith(groupName)
            ? nameInput.value.slice(groupName.length).replace(/^[^\da-z]*/i, '')
            : nameInput.value
          : false;
    // Initial load
    const shortName = getShortName();
    !!shortName && checkAndToggle(shortName);
    nameInput.addEventListener('input', () => {
      const shortName = getShortName();
      !!shortName && checkAndToggle(shortName);
    });
  });
}
