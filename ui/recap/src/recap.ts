import { init, classModule, attributesModule } from 'snabbdom';
import { view, awaiter } from './view';
import type { Recap } from './interfaces';
import { makeSwiper } from './swiper';
import { json } from 'common/xhr';

interface Opts {
  recap?: Recap;
  user: LightUser;
}

const patch = init([classModule, attributesModule]);

export async function initModule(opts: Opts): Promise<void> {
  const getEl = () => document.querySelector('#recap-swiper') as HTMLElement;
  if (opts.recap) {
    patch(getEl(), view(opts.recap, opts.user));
    makeSwiper(opts.recap)(getEl());
  } else {
    patch(getEl(), awaiter(opts.user));
    const loaded: Opts = await json(location.pathname);
    initModule(loaded);
  }
}
