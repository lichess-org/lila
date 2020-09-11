type Fetch<Result> = (term: string) => Promise<Result[]>;

interface Opts<Result> {
  input: HTMLInputElement;
  fetch: Fetch<Result>;
  render: (result: Result) => string;
  select: (result: Result) => string; // return the new input content
  empty?: () => string;
  minLength?: number;
  regex?: RegExp;
}

export default function <Result>(opts: Opts<Result>) {

  const minLength = opts.minLength || 3,
    empty = opts.empty || (() => '<div class="complete-list__empty">No results.</div>'),
    cache = new Map<string, Result[]>(),
    getResults: Fetch<Result> = term => {
      if (cache.has(term)) return new Promise(res => setTimeout(() => res(cache.get(term)), 50));
      else if (
        term.length > 3 && Array.from({
          length: term.length - 3
        }, (_, i) => -i - 1).map(i => term.slice(0, i)).some(sub =>
          cache.has(sub) && !cache.get(sub)!.length
        )
      ) return Promise.resolve([]);
      return opts.fetch(term).then(results => {
        cache.set(term, results);
        return results;
      });
    }

  let $container: Cash = $('<div class="complete-list none"></div>').insertAfter(opts.input);

  opts.input.autocomplete = 'off';

  const update = () => {
    const term = opts.input.value.trim();
    if (term.length >= minLength && (!opts.regex || term.match(opts.regex)))
      getResults(term).then(renderResults);
    else $container.addClass('none');
  }

  $(opts.input).on({
    input: update,
    focus: update,
    // must be delayed, otherwise the result click event doesn't fire
    blur() { setTimeout(() => $container.addClass('none'), 100); return true; }
  });

  const renderResults = (results: Result[]) => {
    $container.empty();
    if (results[0])
      results.forEach(result =>
        $(opts.render(result))
          .on('click', () => {
            /* crazy shit here
               just `opts.input.value = opts.select(result);`
               does nothing. `opts.select` is not called.
               */
            const newValue = opts.select(result);
            opts.input.value = newValue;
            return true;
          })
          .appendTo($container)
      );
    else $container.html(empty());
    $container.removeClass('none');
  };
}
