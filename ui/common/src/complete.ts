interface Opts<Result> {
  input: HTMLInputElement;
  fetch: (term: string) => Promise<Result[]>;
  render: (result: Result) => string;
}

export default function<Result>(opts: Opts<Result>) {

  let $container: Cash | undefined;

  opts.input.addEventListener('input', () => 
    opts.fetch(opts.input.value.trim()).then(renderResults)
  );

  const renderResults = (results: Result[]) => {
    if (!$container) $container = $('<div class="complete-list"></div>').insertAfter(opts.input);
    $container.html(
      results.map(opts.render).join('')
    );
  };

  renderResults([]);
}
