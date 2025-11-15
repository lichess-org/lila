import type { MaybeVNode } from 'lib/view';
import { h } from 'snabbdom';
import type LobbyController from '@/ctrl';
import { speeds, variants } from '@/options';

export const ratingView = (ctrl: LobbyController): MaybeVNode => {
  const { opts, data } = ctrl;
  if (site.blindMode || !data.ratingMap) return null;

  const selectedPerf = ctrl.setupCtrl.selectedPerf();

  const perfOrSpeed: { key: string; icon: string; name: string } | undefined =
    variants.find(({ key }) => key === selectedPerf) || speeds.find(({ key }) => key === selectedPerf);

  if (perfOrSpeed) {
    const perfIconAttrs = { attrs: { 'data-icon': perfOrSpeed.icon } };
    return h(
      'div.ratings',
      !opts.showRatings
        ? [h('i', perfIconAttrs), perfOrSpeed.name]
        : [
            ...i18n.site.yourRatingIsX.asArray(
              h(
                'strong',
                perfIconAttrs,
                data.ratingMap[selectedPerf].rating + (data.ratingMap[selectedPerf].prov ? '?' : ''),
              ),
            ),
            perfOrSpeed.name,
          ],
    );
  }
  return undefined;
};
