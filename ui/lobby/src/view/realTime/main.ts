import * as list from './list';
import * as chart from './chart';
import * as filterView from './filter';
import LobbyController from '../../ctrl';

export default function (ctrl: LobbyController) {
  let filterBody, body, nbFiltered, modeToggle, res;
  if (ctrl.filter.open) filterBody = filterView.render(ctrl);
  switch (ctrl.mode) {
    case 'chart':
      res = ctrl.filter.filter(ctrl.data.hooks);
      nbFiltered = res.hidden;
      body = filterBody || chart.render(ctrl, res.visible);
      modeToggle = ctrl.filter.open ? null : chart.toggle(ctrl);
      break;
    default:
      res = ctrl.filter.filter(ctrl.stepHooks);
      nbFiltered = res.hidden;
      body = filterBody || list.render(ctrl, res.visible);
      modeToggle = ctrl.filter.open ? null : list.toggle(ctrl);
  }
  const filterToggle = filterView.toggle(ctrl, nbFiltered);
  return [filterToggle, modeToggle, body];
}
