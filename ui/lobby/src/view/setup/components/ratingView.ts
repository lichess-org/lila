import { h } from 'snabbdom';

import { perfName } from 'lib/game/perf';
import perfIcons from 'lib/game/perfIcons';
import { dataIcon, icon, type MaybeVNode } from 'lib/view';

import type LobbyController from '@/ctrl';

export const ratingView = ({ opts, data, setupCtrl }: LobbyController): MaybeVNode => {
  if (site.blindMode || !data.ratingMap) return null;

  const perf = setupCtrl.selectedPerf();

  if (!perf) return undefined;

  return h(
    'div.ratings',
    !opts.showRatings
      ? [icon(perfIcons[perf])(), perfName(perf)]
      : [
          ...i18n.site.yourRatingIsX.asArray(
            h(
              'strong',
              { attrs: dataIcon(perfIcons[perf]) },
              setupCtrl.myRating() + (setupCtrl.isProvisional() ? '?' : ''),
            ),
          ),
          perfName(perf),
        ],
  );
};
