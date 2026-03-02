import { debounce } from 'lib/async';
import type { TreeNode } from 'lib/tree/types';
import { storedBooleanPropWithEffect } from 'lib/storage';
import { enter } from 'lib/view';
import { wikiBooksUrl, apiArgs, transformWikiHtml } from 'lib/wikiBooks';

export type WikiTheory = (nodes: TreeNode[]) => void;

export function wikiToggleBox() {
  $('#wikibook-field').each(function (this: HTMLElement) {
    const box = this;

    const state = storedBooleanPropWithEffect('analyse.wikibooks.display', true, value =>
      box.classList.toggle('toggle-box--toggle-off', value),
    );

    const toggle = () => state(!state());

    if (!state()) box.classList.add('toggle-box--toggle-off');

    $(box).children('legend').on('click', toggle).on('keypress', enter(toggle));
  });
}

export default function wikiTheory(): WikiTheory {
  const cache = new Map<string, string>();
  const show = (html: string) => {
    $('.analyse__wiki').toggleClass('empty', !html);
    $('.analyse__wiki-text').html(html);
  };

  const plyPrefix = (node: TreeNode) =>
    `${Math.floor((node.ply + 1) / 2)}${node.ply % 2 === 1 ? '._' : '...'}`;

  return debounce(
    async (nodes: TreeNode[]) => {
      const pathParts = nodes.slice(1).map(n => `${plyPrefix(n)}${n.san}`);
      const path = pathParts.join('/').replace(/[+!#?]/g, '') ?? '';
      if (pathParts.length > 30 || !path || path.length > 255 - 21) show('');
      else if (cache.has(path)) show(cache.get(path)!);
      else if (
        Array.from({ length: pathParts.length }, (_, i) => -i - 1)
          .map(i => pathParts.slice(0, i).join('/'))
          .some(sub => cache.has(sub) && !cache.get(sub)!.length)
      )
        show('');
      else {
        const title = `Chess_Opening_Theory/${path}`;
        try {
          const res = await fetch(`${wikiBooksUrl}/w/api.php?titles=${title}&${apiArgs}`);
          const saveAndShow = (html: string) => {
            cache.set(path, html);
            show(html);
          };
          if (res.ok) {
            const json = await res.json();
            const page = json.query.pages[0];
            if (page.missing || page.extract.length == 0) saveAndShow('');
            else if (page.invalid) show('invalid request: ' + page.invalidreason);
            else if (!page.extract)
              show('error: unexpected API response:<br><pre>' + JSON.stringify(page) + '</pre>');
            else saveAndShow(transformWikiHtml(page.extract, title));
          } else saveAndShow('');
        } catch (err) {
          show('error: ' + err);
        }
      }
    },
    500,
    true,
  );
}

export function wikiClear() {
  $('.analyse__wiki').toggleClass('empty', true);
}
