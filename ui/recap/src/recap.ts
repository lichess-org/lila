import { init, classModule, attributesModule } from 'snabbdom';

import { isMobile } from 'lib/device';
import { json } from 'lib/xhr';

import type { Opts } from './interfaces';
import { makeSwiper } from './swiper';
import { view, awaiter } from './view';

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
