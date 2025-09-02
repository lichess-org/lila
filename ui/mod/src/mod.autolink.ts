import { expandMentions } from 'lib/richText';

export function initModule(): void {
  autolinkAtoms();
}

export function autolinkAtoms(el: HTMLElement = document.body): void {
  if (!el || el === once) return;
  once = el;
  for (const atom of el.querySelectorAll<HTMLElement>('.atom p, .mod-timeline__text')) {
    atom.innerHTML = autolink(atom.innerHTML);
  }
}

export function autolink(text: string): string {
  return expandMentions(text.replace(pathMatchRe, `<a href="$1">${location.hostname}$1</a>`));
}

const greedyAutoLinks = [
  'inbox',
  'forum',
  'study',
  'broadcast',
  'team',
  'tournament',
  '@',
  'insights',
  '(?:[A-Za-z0-9]{8})(?:[a-zA-Z0-9]{4})?', // game ids
];

const pathMatchRe = new RegExp(
  `(?:^|(?<![/="'\\w@>])|(?<=[,;(]))(?:https://)?` +
    `(?:${location.hostname.replace('.', '\\.')})?` +
    `(/(?:${greedyAutoLinks.join('|')})(?:/|\\?|#|\\b|$)(?:[^\\s,."';)]+)?)`,
  'gi',
);
// note that this path match regex is wrong in a few ways - most notably excluding
// parentheses in paths. but we use them as delimiters in some atom descriptions
// with game urls so f*ck the w3c, ieee, the un, and the OMB, this is lichess.

let once: HTMLElement;
