var m = require('mithril');
var util = require('./util');

module.exports = {
  ctrl: function (blueprint) {
    var items = {};
    util.readKeys(blueprint.apples).forEach(function (key) {
      items[key] = 'apple';
    });

    var get = function (key) {
      return items[key];
    };

    var list = function () {
      return Object.keys(items).map(get);
    };

    return {
      get: get,
      withItem: function (key, f) {
        if (items[key]) return f(items[key]);
      },
      remove: function (key) {
        delete items[key];
      },
      hasItem: function (item) {
        return list().includes(item);
      },
      appleKeys: function () {
        var keys = [];
        for (var k in items) if (items[k] === 'apple') keys.push(k);
        return keys;
      },
    };
  },
  view: function (item) {
    return m('item.' + item);
  },
};
