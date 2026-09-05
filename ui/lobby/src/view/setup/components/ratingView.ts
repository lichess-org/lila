import { h } from 'snabbdom';

import { perfNames } from 'lib/game/perf';
import perfIcons from 'lib/game/perfIcons';
import { dataIcon, icon, type MaybeVNode } from 'lib/view';

import type LobbyController from '@/ctrl';
import { speeds, variants } from '@/options';

export const ratingView = ({ opts, data, setupCtrl }: LobbyController): MaybeVNode => {
  if (site.blindMode || !data.ratingMap) return null;

  const selectedPerf = setupCtrl.selectedPerf();
  const perfOrSpeed =
    variants.find(({ key }) => key === selectedPerf) || speeds.find(({ key }) => key === selectedPerf);

  if (!perfOrSpeed) return undefined;
  const perfKey = perfOrSpeed.key;

  return h(
    'div.ratings',
    !opts.showRatings
      ? [icon(perfIcons[perfKey])(), perfNames[perfKey]]
      : [
          ...i18n.site.yourRatingIsX.asArray(
            h(
              'strong',
              { attrs: dataIcon(perfIcons[perfKey]) },
              setupCtrl.myRating() + (setupCtrl.isProvisional() ? '?' : ''),
            ),
          ),
          perfNames[perfKey],
        ],
  );
};
