import { h } from 'snabbdom';

import { dataIcon, iconTag, type MaybeVNode } from 'lib/view';

import type LobbyController from '@/ctrl';
import { speeds, variants } from '@/options';

export const ratingView = ({ opts, data, setupCtrl }: LobbyController): MaybeVNode => {
  if (site.blindMode || !data.ratingMap) return null;

  const selectedPerf = setupCtrl.selectedPerf();
  const perfOrSpeed =
    variants.find(({ key }) => key === selectedPerf) || speeds.find(({ key }) => key === selectedPerf);

  if (!perfOrSpeed) return undefined;

  return h(
    'div.ratings',
    !opts.showRatings
      ? [iconTag(perfOrSpeed.icon), perfOrSpeed.name]
      : [
          ...i18n.site.yourRatingIsX.asArray(
            h(
              'strong',
              { attrs: dataIcon(perfOrSpeed.icon) },
              setupCtrl.myRating() + (setupCtrl.isProvisional() ? '?' : ''),
            ),
          ),
          perfOrSpeed.name,
        ],
  );
};
