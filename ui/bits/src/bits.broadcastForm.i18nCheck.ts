export default function initModule(): void {
  const nameInput = document.querySelector<HTMLInputElement>('#form3-name');
  if (!nameInput) return;
  const helpEl = document.querySelector<HTMLElement>('.relay-name-help')!;
  const isRoundEdit = !!document.querySelector('form.relay-round-form');
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
    /^men$/i,
    /^girls$/i,
    /^boys$/i,
    /^open\s+u(\d+)$/i,
    /^girls\s+u(\d+)$/i,
    /^boys\s+u(\d+)$/i,
    /^quarter[-\s]?final[s]?$/i,
    /^semi[-\s]?final[s]?$/i,
    /^final[s]?$/i,
    /^tie[-\s]?break(?:er)?[s]?$/i,
    /^knock[-\s]?out[s]?$/i,
  ];
  const checkAndToggle = (name: string) => {
    const translatedTokens = name
      .split(/\s+\|\s+/)
      .filter(token => regexes.some(re => re.test(token)))
      .map(token => `"${token}"`);
    if (translatedTokens.length) {
      helpEl.innerHTML = i18n.broadcast.termsAutomaticallyTranslated(translatedTokens.join(', ')) + '<br>';
      helpEl.classList.toggle('none', false);
    } else helpEl.classList.toggle('none', true);
  };
  const getShortName = () => {
    const name = isRoundEdit
      ? nameInput.value
      : groupName
        ? nameInput.value.startsWith(groupName)
          ? nameInput.value.slice(groupName.length).replace(/^[^\da-z]*/i, '')
          : nameInput.value
        : false;
    if (name) return name.trim();
  };
  // Initial load
  const shortName = getShortName();
  !!shortName && checkAndToggle(shortName);
  nameInput.addEventListener('input', () => {
    const shortName = getShortName();
    !!shortName && checkAndToggle(shortName);
  });
}
