import m from 'mithril';
import util from './util';
import text from '../text';
import pairings from './pairings';
import results from './results';

export default function (ctrl) {
  return [
    m('div.box__top', [util.title(ctrl), m('div.box__top__actions', m('div.finished', ctrl.trans('finished')))]),
    text.view(ctrl),
    results(ctrl),
    pairings(ctrl),
  ];
};
