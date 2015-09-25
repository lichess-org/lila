var m = require('mithril');

module.exports = function(env) {

  this.data = env.data;

  this.userId = env.userId;

  this.update = function(data) {
    this.data = data;
    m.redraw();
  }.bind(this);

  this.trans = lichess.trans(env.i18n);

  setInterval(m.redraw, 3700);
};
