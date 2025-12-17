import { init, classModule, attributesModule } from 'snabbdom';
import { view, awaiter } from './view';
import { makeSwiper } from './swiper';
import { json } from 'lib/xhr';
import { isMobile } from 'lib/device';
import type { Opts } from './interfaces';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: Opts): Promise<void> {
  opts.navigation = !isMobile();
  const getEl = () => document.querySelector('#recap-swiper') as HTMLElement;
  if (opts.recap) {
    patch(getEl(), view(opts.recap, opts));
    makeSwiper(opts)(getEl());
  } else {
    patch(getEl(), awaiter(opts.user));
    const loaded: Opts = await json(location.pathname);
    initModule(loaded);
  }
}
