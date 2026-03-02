import type { MaybeVNode } from 'lib/view';
import { h } from 'snabbdom';
import type LobbyController from '@/ctrl';
import { speeds, variants } from '@/options';

export const ratingView = ({ opts, data, setupCtrl }: LobbyController): MaybeVNode => {
  if (site.blindMode || !data.ratingMap) return null;

  const selectedPerf = setupCtrl.selectedPerf();
  const perfOrSpeed =
    variants.find(({ key }) => key === selectedPerf) || speeds.find(({ key }) => key === selectedPerf);

  if (!perfOrSpeed) return undefined;

  const perfIconAttrs = { attrs: { 'data-icon': perfOrSpeed.icon } };
  return h(
    'div.ratings',
    !opts.showRatings
      ? [h('i', perfIconAttrs), perfOrSpeed.name]
      : [
          ...i18n.site.yourRatingIsX.asArray(
            h('strong', perfIconAttrs, setupCtrl.myRating() + (setupCtrl.isProvisional() ? '?' : '')),
          ),
          perfOrSpeed.name,
        ],
  );
};
