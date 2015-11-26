var m = require('mithril');
var throttle = require('./throttle');

module.exports = function(env) {

  this.ui = env.ui;
  this.userId = env.userId;

  this.vm = {
    metric: env.ui.metrics[0],
    dimension: env.ui.dimensions[0],
    filters: {},
    answer: null
  };

  var askQuestion = throttle(1000, false, function() {
    this.vm.answer = null;
    if (!this.validCombinationCurrent()) return;
    m.request({
      method: 'post',
      url: '/coach/data/' + this.userId,
      data: {
        metric: this.vm.metric.key,
        dimension: this.vm.dimension.key,
        filters: this.vm.filters
      }
    }).then(function(answer) {
      this.vm.answer = answer;
    }.bind(this));
    m.redraw();
  }.bind(this));

  this.validCombination = function(dimension, metric) {
    return dimension.position === 'game' || metric.position === 'move';
  };
  this.validCombinationCurrent = function() {
    return this.validCombination(this.vm.dimension, this.vm.metric);
  }.bind(this);

  this.setMetric = function(key) {
    this.vm.metric = this.ui.metrics.filter(function(x) {
      return x.key === key;
    })[0];
    askQuestion();
  }.bind(this);

  this.setDimension = function(key) {
    this.vm.dimension = this.ui.dimensions.filter(function(x) {
      return x.key === key;
    })[0];
    askQuestion();
  }.bind(this);

  this.setFilter = function(dimensionKey, valueKeys) {
    this.vm.filters[dimensionKey] = valueKeys;
    askQuestion();
  }.bind(this);

  this.trans = lichess.trans(env.i18n);

  askQuestion();
};
