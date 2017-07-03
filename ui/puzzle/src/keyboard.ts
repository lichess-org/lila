import * as control from './control';

function preventing(f: () => void): (e: MouseEvent) => void {
  return function(e) {
    if (e.preventDefault) {
      e.preventDefault();
    } else {
      // internet explorer
      e.returnValue = false;
    }
    f();
  };
}

export default function(ctrl) {
  if (!window.Mousetrap) return;
  var kbd = window.Mousetrap;
  kbd.bind(['left', 'k'], preventing(function() {
    control.prev(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['right', 'j'], preventing(function() {
    control.next(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['up', '0'], preventing(function() {
    control.first(ctrl);
    ctrl.redraw();
  }));
  kbd.bind(['down', '$'], preventing(function() {
    control.last(ctrl);
    ctrl.redraw();
  }));
  kbd.bind('l', preventing(ctrl.toggleCeval));
  kbd.bind('x', preventing(ctrl.toggleThreatMode));
  kbd.bind('space', preventing(function() {
    if (ctrl.vm.mode !== 'view') return;
    if (ctrl.getCeval().enabled()) ctrl.playBestMove();
    else ctrl.toggleCeval();
  }));
}
