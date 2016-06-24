var m = require('mithril');

module.exports = {
  ctrl: function(blueprint) {

    var items = {};
    for (var k in blueprint) items[k] = blueprint[k];

    var get = function(key) {
      return items[key];
    };

    var list = function() {
      return Object.keys(items).map(get);
    };

    var hasItem = function(item) {
      return function(i) {
        return i === item;
      };
    };

    return {
      get: get,
      withItem: function(key, f) {
        if (items[key]) return f(items[key]);
      },
      remove: function(key) {
        delete items[key];
      },
      hasItem: function(item) {
        return list().indexOf(item) !== -1;
      },
      flowerKey: function() {
        for (var k in items)
          if (items[k] === 'flower') return k;
      }
    };
  },
  view: function(item) {
    return m('item.' + item);
  }
};
