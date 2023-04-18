import { MaybeVNode } from 'common/snabbdom';
import { h } from 'snabbdom';
import LobbyController from '../../../ctrl';
import { speeds, variants } from '../../../options';

export const ratingView = (ctrl: LobbyController): MaybeVNode => {
  const { opts, data } = ctrl;
  if (opts.blindMode || !data.ratingMap) return null;

  const selectedPerf = ctrl.setupCtrl.selectedPerf();

  const perfOrSpeed: { key: string; icon: string; name: string } | undefined =
    variants.find(({ key }) => key === selectedPerf) || speeds.find(({ key }) => key === selectedPerf);

  return (
    perfOrSpeed &&
    h(
      'div.ratings',
      opts.hideRatings
        ? [h('i', { attrs: { 'data-icon': perfOrSpeed.icon } }), perfOrSpeed.name]
        : [
            ...ctrl.trans.vdom(
              'perfRatingX',
              h('strong', { attrs: { 'data-icon': perfOrSpeed.icon } }, data.ratingMap[selectedPerf].rating)
            ),
            perfOrSpeed.name,
          ]
    )
  );
};
