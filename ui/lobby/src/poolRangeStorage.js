function makeKey(poolId) {
  return 'lobby-pool-range-' + poolId;
}

module.exports = {

  set: function(poolId, range) {
    var key = makeKey(poolId);
    if (range) lichess.storage.set(key, range);
    else lichess.storage.remove(key);
  },

  get: function(poolId) {
    return lichess.storage.get(makeKey(poolId));
  }
};
