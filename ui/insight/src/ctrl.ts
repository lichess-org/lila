import { throttlePromiseDelay } from 'common/throttle';
import * as xhr from 'common/xhr';
import {
  InsightData,
  Dimension,
  Env,
  Metric,
  Question,
  UI,
  EnvUser,
  Vm,
  Filters,
  ViewTab,
} from './interfaces';
import { isLandscapeLayout } from './view';

export default class {
  env: Env;
  ui: UI;
  user: EnvUser;
  own: boolean;
  isUserAction = false;
  domElement: Element;
  redraw: () => void;

  dimensions: Dimension[];
  metrics: Metric[];

  vm: Vm;

  constructor(env: Env, domElement: Element, redraw: () => void) {
    this.env = env;
    this.ui = env.ui;
    this.user = env.user;
    this.own = env.myUserId === env.user.id;
    this.domElement = domElement;
    this.redraw = redraw;

    this.dimensions = Array.prototype.concat.apply(
      [],
      env.ui.dimensionCategs.map(c => c.items),
    );
    this.metrics = Array.prototype.concat.apply(
      [],
      env.ui.metricCategs.map(c => c.items),
    );

    this.vm = {
      metric: this.findMetric(this.env.initialQuestion.metric)!,
      dimension: this.findDimension(this.env.initialQuestion.dimension)!,
      filters: this.env.initialQuestion.filters,
      loading: true,
      broken: false,
      answer: null,
      panel: Object.keys(env.initialQuestion.filters).length ? 'filter' : 'preset',
      view: isLandscapeLayout() ? 'combined' : 'presets',
    };
  }

  private findMetric = (key: string) => this.metrics.find(x => x.key === key);

  private findDimension = (key: string) => this.dimensions.find(x => x.key === key);

  setPanel(p: 'filter' | 'preset') {
    this.vm.panel = p;
    this.redraw();
  }

  setView(view: ViewTab) {
    this.vm.view = view;
    this.redraw();
  }

  reset() {
    this.vm.metric = this.metrics[0];
    this.vm.dimension = this.dimensions[0];
    this.vm.filters = {};
  }

  askQuestion = throttlePromiseDelay(
    () => 1000,
    () => {
      if (!this.validCombinationCurrent()) this.reset();
      this.pushState();
      this.vm.loading = true;
      this.vm.broken = false;
      this.redraw();
      return new Promise<void>(resolve => {
        setTimeout(
          () =>
            xhr
              .json(this.env.postUrl, {
                method: 'post',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                  metric: this.vm.metric.key,
                  dimension: this.vm.dimension.key,
                  filters: this.vm.filters,
                }),
              })
              .then(
                (answer: InsightData) => {
                  this.vm.answer = answer;
                  this.vm.loading = false;
                  if (this.isUserAction) this.vm.view = 'insights';
                  this.isUserAction = false;
                  this.redraw();
                },
                () => {
                  this.isUserAction = false;
                  this.vm.loading = false;
                  this.vm.broken = true;
                  this.redraw();
                },
              )
              .finally(resolve),
          1,
        );
      });
    },
  );

  makeUrl(dKey: string, mKey: string, filters: Filters) {
    const url = [this.env.pageUrl, mKey, dKey].join('/');
    const filtersStr = Object.entries(filters)
      .filter(([_, values]) => !!values)
      .map(([name, values]) => name + ':' + values.join(','))
      .join('/');
    return filtersStr.length ? url + '/' + filtersStr : url;
  }

  makeCurrentUrl() {
    return this.makeUrl(this.vm.dimension.key, this.vm.metric.key, this.vm.filters);
  }

  pushState() {
    history.replaceState({}, '', this.makeCurrentUrl());
  }

  validCombination(dimension?: Dimension, metric?: Metric) {
    return dimension && metric && (dimension.position === 'game' || metric.position === 'move');
  }
  validCombinationCurrent() {
    return this.validCombination(this.vm.dimension, this.vm.metric);
  }

  setMetric(key: string) {
    this.vm.metric = this.findMetric(key)!;
    if (!this.validCombinationCurrent())
      this.vm.dimension = this.dimensions.find(d => this.validCombination(d, this.vm.metric))!;
    this.vm.panel = 'filter';
    this.askQuestion();
  }

  setDimension(key: string) {
    this.vm.dimension = this.findDimension(key)!;
    if (!this.validCombinationCurrent())
      this.vm.metric = this.metrics.find(m => this.validCombination(this.vm.dimension, m))!;
    this.vm.panel = 'filter';
    this.askQuestion();
  }

  setFilter(dimensionKey: string, valueKeys: string[]) {
    if (dimensionKey == 'period' && valueKeys[0] == '3650') valueKeys = []; // 10 years
    if (!valueKeys.length) delete this.vm.filters[dimensionKey];
    else this.vm.filters[dimensionKey] = valueKeys;
    this.askQuestion();
  }

  setQuestion(q: Question) {
    this.vm.dimension = this.findDimension(q.dimension)!;
    this.vm.metric = this.findMetric(q.metric)!;
    this.vm.filters = {
      ...q.filters,
      variant: (this.vm.view === 'combined' && this.vm.filters.variant) || q.filters.variant,
    };
    this.isUserAction = true;
    this.askQuestion();
    $(this.domElement).find('select.ms').multipleSelect('open');
    setTimeout(() => {
      $(this.domElement).find('select.ms').multipleSelect('close');
    }, 1000);
  }

  clearFilters() {
    if (Object.keys(this.vm.filters).length) {
      this.vm.filters = {};
      this.askQuestion();
    }
  }
  // this.trans = site.trans(env.i18n);
}
