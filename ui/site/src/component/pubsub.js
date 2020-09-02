lichess.pubsub = (function() {
  let subs = [];
  return {
    on(name, cb) {
      subs[name] = subs[name] || [];
      subs[name].push(cb);
    },
    off(name, cb) {
      if (!subs[name]) return;
      for (var i in subs[name]) {
        if (subs[name][i] === cb) {
          subs[name].splice(i);
          break;
        }
      }
    },
    emit(name /*, args... */ ) {
      if (!subs[name]) return;
      const args = Array.prototype.slice.call(arguments, 1);
      for (let i in subs[name]) subs[name][i].apply(null, args);
    }
  };
})();
