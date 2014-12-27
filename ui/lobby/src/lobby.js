function hookOrder(a, b) {
  return a.rating > b.rating ? -1 : 1;
};

module.exports = {
  sortHooks: function(hooks) {
    hooks.sort(hookOrder);
  }
};
