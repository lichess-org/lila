var m = require('mithril');

module.exports = {
  builder: {
    apple: function() {
      return {
        type: 'apple'
      };
    }
  },
  view: function(item) {
    switch (item.type) {
      case 'apple':
        return m('apple');
    }
  }
};
