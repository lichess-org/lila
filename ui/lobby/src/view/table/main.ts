import LobbyController from '../../ctrl';
import * as chart from './chart';
import { createSeek } from './correspondence';
import * as filterView from './filter';
import * as list from './list';

export default function (ctrl: LobbyController) {
  let filterBody, body, nbFiltered, modeToggle, res, button;

  const isSeeks = ctrl.tab === 'seeks';
  if (ctrl.filter.open) filterBody = filterView.render(ctrl);
  switch (ctrl.mode) {
    case 'chart':
      res = isSeeks ? ctrl.filter.filterSeeks(ctrl.data.seeks) : ctrl.filter.filter(ctrl.data.hooks);
      nbFiltered = res.hidden;
      body = filterBody || chart.render(isSeeks ? 'seeks' : 'real_time', ctrl, res.visible);
      modeToggle = ctrl.filter.open ? null : chart.toggle(ctrl);
      break;
    default:
      res = isSeeks ? ctrl.filter.filterSeeks(ctrl.data.seeks) : ctrl.filter.filter(ctrl.stepHooks);
      nbFiltered = res.hidden;
      body = filterBody || list.render(isSeeks ? 'seeks' : 'real_time', ctrl, res.visible);
      modeToggle = ctrl.filter.open ? null : list.toggle(ctrl);
      button = !filterBody && isSeeks && res.visible.length < 10 ? createSeek(ctrl) : null;
  }

  const filterToggle = filterView.toggle(ctrl, nbFiltered);
  return [filterToggle, modeToggle, body, button];
}
