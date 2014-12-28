var tabKey = 'lichess.lobby.tab';

function fixTab(tab) {
  if (['real_time', 'seeks', 'now_playing'].indexOf(tab) === -1) tab = 'real_time';
  return tab;
};

module.exports = {
  tab: {
    set: function(tab) {
      var tab = fixTab(tab);
      storage.set(tabKey, tab);
      return tab;
    },
    get: function() {
      return fixTab(storage.get(tabKey));
    }
  }
};
