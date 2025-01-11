import {
  attributesModule,
  classModule,
  eventListenersModule,
  init,
  propsModule,
  styleModule,
} from 'snabbdom';
import { AnalyseCtrl, type AnalyseData, type StudyData } from '../analyse/ctrl';
import { view } from '../analyse/view/main';

const patch = init([classModule, attributesModule, styleModule, propsModule, eventListenersModule]);

window.lishogi.ready.then(() => {
  const data = window.lishogi.modulesData[__bundlename__].data as AnalyseData;
  const study = window.lishogi.modulesData[__bundlename__].study as StudyData | undefined;
  const el = document.querySelector('main.analyse')!;
  const ctrl = new AnalyseCtrl(data, study, redraw);
  console.log('embed data:', data, window.lishogi.modulesData[__bundlename__]);

  el.innerHTML = '';
  let vnode = patch(el, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
});
