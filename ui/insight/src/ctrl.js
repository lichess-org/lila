var m = require('mithril');
var throttle = require('common/throttle').default;

module.exports = function (env, domElement) {
  this.ui = env.ui;
  this.user = env.user;
  this.own = env.myUserId === this.user.id;
  this.dimensions = [].concat.apply(
    [],
    this.ui.dimensionCategs.map(function (c) {
      return c.items;
    })
  );
  this.metrics = [].concat.apply(
    [],
    this.ui.metricCategs.map(function (c) {
      return c.items;
    })
  );

  var findMetric = function (key) {
    return this.metrics.find(function (x) {
      return x.key === key;
    });
  }.bind(this);

  var findDimension = function (key) {
    return this.dimensions.find(function (x) {
      return x.key === key;
    });
  }.bind(this);

  this.vm = {
    metric: findMetric(env.initialQuestion.metric),
    dimension: findDimension(env.initialQuestion.dimension),
    filters: env.initialQuestion.filters,
    loading: true,
    broken: false,
    answer: null,
    panel: Object.keys(env.initialQuestion.filters).length ? 'filter' : 'preset',
  };

  this.setPanel = function (p) {
    this.vm.panel = p;
    m.redraw();
  }.bind(this);

  var reset = function () {
    this.vm.metric = this.metrics[0];
    this.vm.dimension = this.dimensions[0];
    this.vm.filters = {};
  }.bind(this);

  var askQuestion = throttle(
    1000,
    function () {
      if (!this.validCombinationCurrent()) reset();
      this.pushState();
      this.vm.loading = true;
      this.vm.broken = false;
      m.redraw();
      setTimeout(
        function () {
          m.request({
            method: 'post',
            url: env.postUrl,
            data: {
              metric: this.vm.metric.key,
              dimension: this.vm.dimension.key,
              filters: this.vm.filters,
            },
            deserialize: function (d) {
              try {
                return JSON.parse(d);
              } catch (e) {
                throw new Error(d);
              }
            },
          }).then(
            function (answer) {
              this.vm.answer = answer;
              this.vm.loading = false;
            }.bind(this),
            function () {
              this.vm.loading = false;
              this.vm.broken = true;
              m.redraw();
            }.bind(this)
          );
        }.bind(this),
        1
      );
    }.bind(this)
  );

  this.makeUrl = function (dKey, mKey, filters) {
    var url = [env.pageUrl, mKey, dKey].join('/');
    var filters = Object.keys(filters)
      .map(function (filterKey) {
        return filterKey + ':' + filters[filterKey].join(',');
      })
      .join('/');
    if (filters.length) url += '/' + filters;
    return url;
  };

  this.makeCurrentUrl = function () {
    return this.makeUrl(this.vm.dimension.key, this.vm.metric.key, this.vm.filters);
  }.bind(this);

  this.pushState = function () {
    history.replaceState({}, null, this.makeCurrentUrl());
  }.bind(this);

  this.validCombination = function (dimension, metric) {
    return dimension && metric && (dimension.position === 'game' || metric.position === 'move');
  };
  this.validCombinationCurrent = function () {
    return this.validCombination(this.vm.dimension, this.vm.metric);
  }.bind(this);

  this.setMetric = function (key) {
    this.vm.metric = findMetric(key);
    if (!this.validCombinationCurrent())
      this.vm.dimension = this.dimensions.find(
        function (d) {
          return this.validCombination(d, this.vm.metric);
        }.bind(this)
      );
    this.vm.panel = 'filter';
    askQuestion();
  }.bind(this);

  this.setDimension = function (key) {
    this.vm.dimension = findDimension(key);
    if (!this.validCombinationCurrent())
      this.vm.metric = this.metrics.find(
        function (m) {
          return this.validCombination(this.vm.dimension, m);
        }.bind(this)
      );
    this.vm.panel = 'filter';
    askQuestion();
  }.bind(this);

  this.setFilter = function (dimensionKey, valueKeys) {
    if (!valueKeys.length) delete this.vm.filters[dimensionKey];
    else this.vm.filters[dimensionKey] = valueKeys;
    askQuestion();
  }.bind(this);

  this.setQuestion = function (q) {
    this.vm.dimension = findDimension(q.dimension);
    this.vm.metric = findMetric(q.metric);
    this.vm.filters = q.filters;
    askQuestion();
    $(domElement).find('select.ms').multipleSelect('open');
    setTimeout(function () {
      $(domElement).find('select.ms').multipleSelect('close');
    }, 1000);
  }.bind(this);

  this.clearFilters = function () {
    if (Object.keys(this.vm.filters).length) {
      this.vm.filters = {};
      askQuestion();
    }
  }.bind(this);

  // this.trans = lichess.trans(env.i18n);

  askQuestion();
};
