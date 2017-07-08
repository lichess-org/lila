import * as list from './list';
import * as chart from './chart';
import filter from '../../filter';
import * as filterView from './filter';
import LobbyController from '../../ctrl';

export default function(ctrl: LobbyController) {
  let filterBody, body, nbFiltered, modeToggle, res;
  if (ctrl.filterOpen) filterBody = filterView.render(ctrl);
  switch (ctrl.mode) {
    case 'chart':
      res = filter(ctrl, ctrl.data.hooks);
      nbFiltered = res.hidden;
      body = filterBody || chart.render(ctrl, res.visible);
      modeToggle = ctrl.filterOpen ? null : chart.toggle(ctrl);
      break;
    default:
      res = filter(ctrl, ctrl.stepHooks);
      nbFiltered = res.hidden;
      body = filterBody || list.render(ctrl, res.visible);
      modeToggle = ctrl.filterOpen ? null : list.toggle(ctrl);
  }
  const filterToggle = filterView.toggle(ctrl, nbFiltered);
  return [
    filterToggle,
    modeToggle,
    body
  ];
}
