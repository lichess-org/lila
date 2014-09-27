function start(ctrl, e) {
  if (e.button !== 0) return; // only left click
  var role = e.target.getAttribute('data-role')
  color = e.target.getAttribute('data-color');
  if (!role || !color) return;
  e.stopPropagation();
  e.preventDefault();
  var bounds = e.target.parentNode.parentNode.getBoundingClientRect();
  var pieceBounds = e.target.getBoundingClientRect();
  ctrl.data.extra = {
    bounds: bounds,
    role: role,
    color: color,
    left: e.clientX - bounds.left,
    top: e.clientY - bounds.top,
    dec: [
      - pieceBounds.width / 2,
      - pieceBounds.height / 2
    ],
  };
  console.log(ctrl.data.extra);
  m.redraw();
}

function move(ctrl, e) {
  var cur = ctrl.data.extra;
  if (!cur.role) return;
  cur.left = e.clientX - cur.bounds.left;
  cur.top = e.clientY - cur.bounds.top;
  m.redraw();
}

function end(ctrl, e) {
  if (!ctrl.data.extra.role) return;
  ctrl.data.extra = {};
  m.redraw();
}

module.exports = {
  start: start,
  move: move,
  end: end
};
