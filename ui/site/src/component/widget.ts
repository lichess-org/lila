import * as data from 'common/data';

const widget = (name: string, prototype: any) => {
  const constructor = ($[name] = function (options: any, element: HTMLElement) {
    const self: any = this;
    self.element = $(element);
    (element as any)[name] = this;
    self.options = options;
    self._create();
  });
  constructor.prototype = prototype;
  $.fn[name] = function (method: string) {
    const args = Array.prototype.slice.call(arguments, 1);
    if (typeof method === 'string')
      this.each(function (this: HTMLElement) {
        const instance = data.get(this, name);
        if (instance && $.isFunction(instance[method])) instance[method].apply(instance, args);
      });
    else
      this.each(function (this: HTMLElement) {
        if (!data.get(this, name)) data.set(this, name, new constructor(method, this));
      });
    return this;
  };
};

export default widget;
