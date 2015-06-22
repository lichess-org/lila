module.exports = function(ctrl, hooks) {
  var f = ctrl.data.filter;
  var seen = [], hidden = 0, visible = [];
  hooks.forEach(function(hook) {
    if (hook.action === 'cancel') visible.push(hook);
    else {
      if (!$.fp.contains(f.variant, hook.variant.key) || !$.fp.contains(f.mode, hook.mode) || !$.fp.contains(f.speed, hook.speed) ||
        (f.rating && (!hook.rating || (hook.rating < f.rating[0] || hook.rating > f.rating[1])))) {
        hidden++;
      } else {
        var hash = hook.mode + hook.variant.key + hook.time + hook.rating;
        if (!$.fp.contains(seen, hash)) visible.push(hook);
        seen.push(hash);
      }
    }
  });
  return {
    visible: visible,
    hidden: hidden
  };
}
