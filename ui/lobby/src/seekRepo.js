function order(a, b) {
  return a.rating > b.rating ? -1 : 1;
}

function sort(ctrl) {
  ctrl.data.seeks.sort(order);
}

function initAll(ctrl) {
  ctrl.data.seeks.forEach(function(seek) {
    seek.action = (ctrl.data.me && seek.username === ctrl.data.me.username) ? 'cancelSeek' : 'joinSeek';
  });
  sort(ctrl);
}

module.exports = {
  initAll: initAll,
  sort: sort,
  find: function(ctrl, id) {
    return ctrl.data.seeks.filter(function(s) {
      return s.id === id;
    })[0];
  }
};
