var m = require('mithril');
var throttle = require('./throttle');

module.exports = function(env) {

  this.ui = env.ui;
  this.user = {
    id: env.userId,
    name: env.username,
    nbGames: env.nbGames,
    stale: env.stale
  };

  var findMetric = function(key) {
    return this.ui.metrics.filter(function(x) {
      return x.key === key;
    })[0];
  }.bind(this);

  var findDimension = function(key) {
    return this.ui.dimensions.filter(function(x) {
      return x.key === key;
    })[0];
  }.bind(this);

  this.vm = {
    metric: findMetric(env.initialQuestion.metric),
    dimension: findDimension(env.initialQuestion.dimension),
    filters: env.initialQuestion.filters,
    loading: true,
    answer: null
  };

  var askQuestion = throttle(1000, false, function() {
    if (!this.validCombinationCurrent()) return m.redraw();
    this.pushState();
    this.vm.loading = true;
    m.redraw();
    m.request({
      method: 'post',
      url: env.postUrl,
      data: {
        metric: this.vm.metric.key,
        dimension: this.vm.dimension.key,
        filters: this.vm.filters
      }
    }).then(function(answer) {
      this.vm.answer = answer;
      this.vm.loading = false;
    }.bind(this));
  }.bind(this));

  this.pushState = function() {
    var url = [env.pageUrl, this.vm.metric.key, this.vm.dimension.key].join('/');
    var filters = Object.keys(this.vm.filters).map(function(filterKey) {
      return filterKey + ':' + this.vm.filters[filterKey].join(',');
    }.bind(this)).join('/');
    if (filters.length) url += '/' + filters;
    if (window.history.replaceState) window.history.replaceState({}, null, url);
  }.bind(this);

  this.validCombination = function(dimension, metric) {
    return dimension.position === 'game' || metric.position === 'move';
  };
  this.validCombinationCurrent = function() {
    return this.validCombination(this.vm.dimension, this.vm.metric);
  }.bind(this);

  this.setMetric = function(key) {
    this.vm.metric = findMetric(key);
    askQuestion();
  }.bind(this);

  this.setDimension = function(key) {
    this.vm.dimension = findDimension(key);
    askQuestion();
  }.bind(this);

  this.setFilter = function(dimensionKey, valueKeys) {
    if (!valueKeys.length) delete this.vm.filters[dimensionKey];
    else this.vm.filters[dimensionKey] = valueKeys;
    askQuestion();
  }.bind(this);

  this.trans = lichess.trans(env.i18n);

  askQuestion();
};
