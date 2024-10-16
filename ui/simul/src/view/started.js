import util from './util';
import text from '../text';
import pairings from './pairings';
import results from './results';

export default function (ctrl) {
  return [util.title(ctrl), text.view(ctrl), results(ctrl), pairings(ctrl)];
};
