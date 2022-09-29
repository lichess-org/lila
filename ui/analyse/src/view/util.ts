import { dataIcon } from 'common/snabbdom';
import { fixCrazySan } from 'chess';
import { h } from 'snabbdom';
import { plyToTurn } from '../util';
import { attributesModule, classModule, init } from 'snabbdom';

export const patch = init([classModule, attributesModule]);

export { autolink, innerHTML, enrichText, richHTML, toYouTubeEmbed, toTwitchEmbed } from 'common/richText';

export const emptyRedButton = 'button.button.button-red.button-empty';

export const iconTag = (icon: string) => h('i', { attrs: dataIcon(icon) });

export const baseUrl = () => `${window.location.protocol}//${window.location.host}`;

export function nodeFullName(node: Tree.Node) {
  if (node.san) return plyToTurn(node.ply) + (node.ply % 2 === 1 ? '.' : '...') + ' ' + fixCrazySan(node.san);
  return 'Initial position';
}

export const plural = (noun: string, nb: number): string => nb + ' ' + (nb === 1 ? noun : noun + 's');

export function titleNameToId(titleName: string): string {
  const split = titleName.split(' ');
  return (split.length === 1 ? split[0] : split[1]).toLowerCase();
}

export const option = (value: string, current: string | undefined, name: string) =>
  h(
    'option',
    {
      attrs: {
        value: value,
        selected: value === current,
      },
    },
    name
  );

export function scrollTo(el: HTMLElement | undefined, target: HTMLElement | null) {
  if (el && target) el.scrollTop = target.offsetTop - el.offsetHeight / 2 + target.offsetHeight / 2;
}
