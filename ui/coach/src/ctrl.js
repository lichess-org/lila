var m = require('mithril');
var throttle = require('./throttle');

module.exports = function(env) {

  this.ui = env.ui;
  this.userId = env.userId;

  this.vm = {
    metric: env.ui.metrics[0].key,
    dimension: 'opponentStrength',
    answer: null
  };

  var askQuestion = throttle(1000, false, function() {
    this.vm.answer = null;
    m.request({
      method: 'post',
      url: '/coach/data/' + this.userId,
      data: {
        metric: this.vm.metric,
        dimension: this.vm.dimension
      }
    }).then(function(answer) {
      console.log(answer);
      this.vm.answer = answer;
    }.bind(this));
  }.bind(this));

  askQuestion();

  this.setMetric = function(key) {
    this.vm.metric = key;
    askQuestion();
  }.bind(this);

  this.setDimension = function(key) {
    this.vm.dimension = key;
    askQuestion();
  }.bind(this);

  this.trans = lichess.trans(env.i18n);
};
