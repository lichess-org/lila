import { frag } from 'lib';
import { text as xhrText } from 'lib/xhr';

export async function markdownPreview(body: string, realm = 'forum'): Promise<HTMLElement> {
  console.log(realm);
  const rendered = frag<HTMLElement>(
    (
      await Promise.all([
        xhrText(`/markdown/preview/${realm}`, { method: 'POST', body }),
        site.asset.loadCssPath('bits.ask'),
      ])
    )[0],
  );
  await Promise.all([
    rendered.querySelector('.lpv--autostart') && site.asset.loadEsm('bits.lpv', { init: { el: rendered } }),
    rendered.querySelector('a') && site.asset.loadEsm('bits.expandText', { init: rendered }),
  ]);
  return rendered;
}
