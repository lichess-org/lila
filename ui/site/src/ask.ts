import * as xhr from 'common/xhr';

lichess.load.then(() => {
  $('div.ask-container').each(function (this: HTMLElement) {
    rewire(this);
  });

  function rewire(container: HTMLElement) {
    const ask = container.firstElementChild as HTMLElement;
    $(ask)
      .find('.ask-xhr')
      .on('click', function (e) {
        xhr.text((e.target as HTMLButtonElement).formAction, { method: 'post' }).then((frag: string) => {
          container!.innerHTML = frag;
          rewire(container);
        });
      });
    return false;
  }
});
