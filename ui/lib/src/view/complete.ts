type Fetch<Result> = (term: string) => Promise<Result[]>;

export interface CompleteOpts<Result> {
  input: HTMLInputElement;
  fetch: Fetch<Result>;
  render: (result: Result) => string | HTMLElement;
  populate: (result: Result) => string; // input value from a search result
  onSelect?: (result: Result) => void;
  empty?: () => string;
  minLength?: number;
  regex?: RegExp;
}

export function complete<Result>(opts: CompleteOpts<Result>): void {
  const minLength = opts.minLength || 3,
    empty = opts.empty || (() => '<div class="complete-list__empty">No results.</div>'),
    cache = new Map<string, Result[]>(),
    fetchResults: Fetch<Result> = async term => {
      if (cache.has(term)) return new Promise(res => setTimeout(() => res(cache.get(term)!), 50));
      else if (
        term.length > 3 &&
        Array.from({ length: term.length - 3 }, (_, i) => -i - 1)
          .map(i => term.slice(0, i))
          .some(sub => cache.has(sub) && !cache.get(sub)!.length)
      )
        return Promise.resolve([]);
      return opts.fetch(term).then(results => {
        cache.set(term, results);
        return results;
      });
    },
    selectedResult = (): Result | undefined => {
      if (selectedIndex === null) return;
      return renderedResults[selectedIndex];
    },
    moveSelection = (offset: number) => {
      const nb = renderedResults.length;
      selectedIndex = (selectedIndex === null ? (offset === 1 ? 0 : -1) : selectedIndex + offset) % nb;
      if (selectedIndex < 0) selectedIndex += nb;
      renderSelection();
      const result = selectedResult();
      if (result) opts.input.value = opts.populate(result);
    },
    renderSelection = () => {
      $container.find('.complete-selected').removeClass('complete-selected');
      if (selectedIndex !== null)
        $container.find('.complete-result').eq(selectedIndex).addClass('complete-selected');
    };

  const $container: Cash = $('<div class="complete-list none"></div>').insertAfter(opts.input);
  let selectedIndex: number | null = null,
    renderedResults: Result[] = [];

  opts.input.autocomplete = 'off';

  const update = () => {
    const term = opts.input.value.trim();
    if (term.length >= minLength && (!opts.regex || term.match(opts.regex)))
      fetchResults(term).then(renderResults, console.log);
    else $container.addClass('none');
  };

  $(opts.input).on({
    input: update,
    focus: update,
    // must be delayed, otherwise the result click event doesn't fire
    blur() {
      setTimeout(() => $container.addClass('none'), 100);
      return true;
    },
    keydown(e: KeyboardEvent) {
      if ($container.hasClass('none')) return;
      if (e.code === 'ArrowDown') {
        moveSelection(1);
        return false;
      }
      if (e.code === 'ArrowUp') {
        moveSelection(-1);
        return false;
      }
      if (e.code === 'Enter') {
        $container.addClass('none');
        const result =
          selectedResult() ||
          (renderedResults[0] && opts.populate(renderedResults[0]) == opts.input.value
            ? renderedResults[0]
            : undefined);
        if (result) {
          if (opts.onSelect) opts.onSelect(result);
          return false;
        }
      }
      return;
    },
  });

  const renderResults = (results: Result[]) => {
    $container.empty();
    if (results[0]) {
      results.forEach(result =>
        $(opts.render(result))
          /* can't use click because blur fires first and removes the click target */
          .on('mousedown touchdown', () => {
            /* crazy shit here
               just `opts.input.value = opts.select(result);`
               does nothing. `opts.select` is not called.
               */
            const newValue = opts.populate(result);
            opts.input.value = newValue;
            if (opts.onSelect) opts.onSelect(result);
            return true;
          })
          .appendTo($container),
      );
    } else $container.html(empty());
    renderedResults = results;
    selectedIndex = null;
    renderSelection();
    $container.removeClass('none');
  };
}
