export const supportedLangs: [string, string][] = [
  ['en', 'English'],
  ['fr', 'Français'],
  ['pl', 'Polski'],
];

if (site.debug)
  supportedLangs.push(
    ['de', 'Deutsch'],
    ['tr', 'Türkçe'],
    ['vi', 'Tiếng Việt'],
    ['ru', 'Русский'],
    ['it', 'Italiano'],
    ['sv', 'Svenska'],
  );
