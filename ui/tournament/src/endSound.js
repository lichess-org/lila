module.exports = function(data) {
  if (!data.me) return;
  if (!data.isRecentlyFinished) return;

  var storageKey = 'tournament.end.sound.' + data.id;
  if (lichess.storage.get(storageKey)) return;
  lichess.storage.set(storageKey, 1);

  var suffixes = ['1st', '2nd', '3rd'];
  var soundKey = 'tournament' + (suffixes[data.me.rank - 1] || 'Other');
  $.sound[soundKey]();
};
