import { json } from 'lib/xhr';

import type { Opts } from './interfaces';
import { play, skeleton } from './player';
import { loading } from './slides';

export async function initModule(opts: Opts): Promise<void> {
  const root = document.querySelector<HTMLElement>('.recap')!;
  root.innerHTML = skeleton();
  if (opts.recap) play(root, opts);
  else {
    // the recap is still being computed: wait for it
    root.querySelector('.stage')!.innerHTML = loading(opts.user);
    const loaded: Opts = await json(location.pathname);
    initModule(loaded);
  }
}
