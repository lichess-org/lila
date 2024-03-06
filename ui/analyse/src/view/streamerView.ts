import { looseH as h } from 'common/snabbdom';
import AnalyseCtrl from '../ctrl';
export function streamerView(ctrl: AnalyseCtrl) {
  ctrl;

  return h('iframe#ytplayer', {
    attrs: {
      type: 'text/html',
      width: '100%',
      height: '220px',
      src: 'https://www.youtube.com/embed/AF8d72mA41M?autoplay=1&origin=https://schlawg.org',
      frameborder: '0',
      allow: 'accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture',
    },
  });
}
