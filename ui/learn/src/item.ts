const m = require('mithril');
const util = require('./util');

module.exports = {
  ctrl: function (blueprint) {
    const items = {};
    util.readKeys(blueprint.apples).forEach(function (key) {
      items[key] = 'apple';
    });

    const get = function (key) {
      return items[key];
    };

    const list = function () {
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
        const keys = [];
        for (const k in items) if (items[k] === 'apple') keys.push(k);
        return keys;
      },
    };
  },
  view: function (item) {
    return m('item.' + item);
  },
};
