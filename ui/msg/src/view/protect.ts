import modal from 'common/modal';

export const linkPopup = (dom: HTMLElement) => {
  console.log(dom);
  $(dom).on('click', 'their a[href^="http"]', function (this: HTMLLinkElement) {
    return onClick(this);
  });
};

const onClick = (a: HTMLLinkElement): boolean => {
  const url = new URL(a.href);
  if (url.host == 'lichess.org') return true;
  console.log(url);
  lichess.loadCssPath('modal');
  modal({
    content: $(
      `<div class="msg-modal">
        <div class="msg-modal__content">
          <div class="msg-modal__content__title">
            <h2>You are leaving Lichess</h2>
            <p class="msg-modal__content__advice">Never type your Lichess password on another site!</p>
          </div>
        </div>
        <div class="msg-modal__actions">
          <a class="cancel">Cancel</a>
          <a href="${a.href}" target="_blank" class="button button-red button-no-upper">Proceed to ${url.host}</a>
        </div>
      </div>`
    ),
    onInsert($wrap) {
      $wrap.find('.cancel').on('click', modal.close);
    },
  });
  return false;
};
