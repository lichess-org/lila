import { MaybeVNode } from 'common/snabbdom';
import { h } from 'snabbdom';
import { SetupCtrl } from '../../ctrl';
import { speeds, variants } from '../../options';

export const ratingView = (ctrl: SetupCtrl): MaybeVNode => {
  if (lichess.blindMode || !ctrl.root.ratingMap) return null;

  const selectedPerf = ctrl.selectedPerf();

  const perfOrSpeed: { key: string; icon: string; name: string } | undefined =
    variants.find(({ key }) => key === selectedPerf) || speeds.find(({ key }) => key === selectedPerf);

  if (perfOrSpeed) {
    const perfIconAttrs = { attrs: { 'data-icon': perfOrSpeed.icon } };
    return h(
      'div.ratings',
      ctrl.root.opts.hideRatings
        ? [h('i', perfIconAttrs), perfOrSpeed.name]
        : [
            ...ctrl.root.trans.vdom(
              'perfRatingX',
              h(
                'strong',
                perfIconAttrs,
                ctrl.root.ratingMap[selectedPerf].rating +
                  (ctrl.root.ratingMap[selectedPerf].prov ? '?' : ''),
              ),
            ),
            perfOrSpeed.name,
          ],
    );
  }
  return undefined;
};
