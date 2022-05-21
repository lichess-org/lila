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
/* {
    const askContainer = this.parent;

    const id = this.getAttribute('id');
    id;
    $(this)
      .find('.ask-xhr')
      .on('click', function (e) {
        const el = e.target as HTMLInputElement;
        var postUrl = el.formAction;
        xhr.text(postUrl, { method: 'post', body: new FormData() }).then((frag: string) => {
          askContainer.innerHTML = frag;
          $(askContainer);
        });
      });
  });
  /*$('form.auto-submit').each(function (this: HTMLFormElement) {
    const form = this;
    const inputElements: Array<HTMLInputElement> = [...this.getElementsByTagName('input')];
    $(form)
      .find('ask-radio')
      .on('input', function (e: InputEvent) {
        inputElements.forEach(function (choiceBtn: HTMLInputElement) {
          if (choiceBtn != e.target) choiceBtn.checked = false;
        });
      });
  });*/
