import * as xhr from 'common/xhr';
import { domDialog } from 'common/dialog';
import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';

site.load.then(() => {
  $('.forum')
    .on('click', 'a.delete', function (this: HTMLAnchorElement) {
      const link = this;
      domDialog({
        cash: $('.forum-delete-modal'),
        attrs: { view: { action: link.href } },
      }).then(dlg => {
        $(dlg.view)
          .find('form')
          .attr('action', link.href)
          .on('submit', function (this: HTMLFormElement, e: Event) {
            e.preventDefault();
            xhr.formToXhr(this);
            $(link).closest('.forum-post').hide();
            dlg.close();
          });
        $(dlg.view).find('form button.cancel').on('click', dlg.close);
        dlg.showModal();
      });
      return false;
    })
    .on('click', 'a.mod-relocate', function (this: HTMLAnchorElement) {
      const link = this;
      domDialog({
        cash: $('.forum-relocate-modal'),
        attrs: { view: { action: link.href } },
      }).then(dlg => {
        $(dlg.view).find('form').attr('action', link.href);
        $(dlg.view).find('form button.cancel').on('click', dlg.close);
        dlg.showModal();
      });
      return false;
    })
    .on('click', 'form.unsub button', function (this: HTMLButtonElement) {
      const form = $(this).parent().toggleClass('on off')[0] as HTMLFormElement;
      xhr.text(`${form.action}?unsub=${this.dataset.unsub}`, { method: 'post' });
      return false;
    });
  $('.forum-post__blocked button').on('click', e => {
    const el = (e.target as HTMLElement).parentElement!;
    $(el).replaceWith($('.forum-post__message', el));
  });
  $('.forum-post__message').each(function (this: HTMLElement) {
    if (this.innerHTML.match(/(^|<br>)&gt;/)) {
      const hiddenQuotes = '<span class=hidden-quotes>&gt;</span>';
      let result = '';
      let quote = [];
      for (const line of this.innerHTML.split('<br>')) {
        if (line.startsWith('&gt;')) quote.push(hiddenQuotes + line.substring(4).trim());
        else {
          if (quote.length > 0) {
            result += `<blockquote>${quote.join('<br>')}</blockquote>`;
            quote = [];
          }
          result += line + '<br>';
        }
      }
      if (quote.length > 0) result += `<blockquote>${quote.join('<br>')}</blockquote>`;
      this.innerHTML = result;
    }
  });

  $('.edit.button')
    .add('.edit-post-cancel')
    .on('click', function (this: HTMLButtonElement, e) {
      e.preventDefault();

      const $post = $(this).closest('.forum-post'),
        $form = $post.find('form.edit-post-form').toggle();
      const $textarea = $form.find('textarea.edit-post-box');
      $textarea.get(0)!.scrollIntoView();

      ($form[0] as HTMLFormElement).reset();
    });

  const quoted = new Set<string>();

  $('.quote.button').on('click', function (this: HTMLButtonElement) {
    const $post = $(this).closest('.forum-post'),
      authorUsername = $post.find('.author').attr('href')?.substring(3),
      author = authorUsername ? '@' + authorUsername : $post.find('.author').text(),
      anchor = $post.find('.anchor').text(),
      message = $post.find('.forum-post__message'),
      response = $('.reply .post-text-area')[0] as HTMLTextAreaElement;

    let messageText = message[0]?.innerText;
    const selection = window.getSelection();
    if (selection && selection.anchorNode?.parentElement === message[0]) messageText = selection.toString();

    message.children('.lpv').each((_, c) => {
      messageText = messageText?.replace(c.innerText ?? '', '');
    });
    let quote = messageText
      ?.replace(/^(?:>.*)\n?|(?:@.+ said in #\d+:\n?)/gm, '')
      .trim()
      .split('\n')
      .map(line => '> ' + line)
      .join('\n');
    quote = `${author} said in ${anchor}:\n${quote}\n`;
    if (!quoted.has(quote)) {
      quoted.add(quote);
      response.value =
        response.value.substring(0, response.selectionStart) +
        quote +
        response.value.substring(response.selectionEnd);
    }
  });

  $('.post-text-area').one('focus', function (this: HTMLTextAreaElement) {
    const textarea = this,
      topicId = $(this).attr('data-topic');

    if (!topicId) return;

    const searchCandidates = function (term: string, candidateUsers: string[]) {
      return candidateUsers.filter((user: string) => user.toLowerCase().startsWith(term.toLowerCase()));
    };

    // We only ask the server for the thread participants once the user has clicked the text box as most hits to the
    // forums will be only to read the thread. So the 'thread participants' starts out empty until the post text area
    // is focused.
    const threadParticipants = xhr.json('/forum/participants/' + topicId);

    new Textcomplete(new TextareaEditor(textarea), [
      {
        index: 2,
        match: /(^|\s)@(|[a-zA-Z_-][\w-]{0,19})$/,
        search: function (term: string, callback: (names: string[]) => void) {
          // Initially we only autocomplete by participants in the thread. As the user types more,
          // we can autocomplete against all users on the site.
          threadParticipants.then(function (participants) {
            const forumParticipantCandidates = searchCandidates(term, participants);

            if (forumParticipantCandidates.length != 0) {
              // We always prefer a match on the forum thread participants' usernames
              callback(forumParticipantCandidates);
            } else if (term.length >= 3) {
              // We fall back to every site user after 3 letters of the username have been entered
              // and there are no matches in the forum thread participants
              xhr
                .json(xhr.url('/api/player/autocomplete', { term }), { cache: 'default' })
                .then(candidateUsers => callback(searchCandidates(term, candidateUsers)))
                .catch(error => {
                  console.error('Autocomplete request failed:', error);
                  callback([]);
                });
            } else {
              callback([]);
            }
          });
        },
        replace: (mention: string) => '$1@' + mention + ' ',
      },
    ]);
  });

  $('.forum').on('click', '.reactions-auth button', e => {
    const href = e.target.getAttribute('data-href');
    if (href) {
      const $rels = $(e.target).parent();
      if ($rels.hasClass('loading')) return;
      $rels.addClass('loading');
      xhr.text(href, { method: 'post' }).then(
        html => {
          $rels.replaceWith(html);
          $rels.removeClass('loading');
        },
        _ => {
          site.announce({ msg: 'Failed to send forum post reaction' });
        },
      );
    }
  });

  const replyStorage = site.tempStorage.make('forum.reply' + location.pathname);
  const replyEl = $('.reply .post-text-area')[0] as HTMLTextAreaElement | undefined;
  let submittingReply = false;

  window.addEventListener('pageshow', () => {
    const storedReply = replyStorage.get();
    if (replyEl && storedReply) replyEl.value = storedReply;
  });

  window.addEventListener('pagehide', () => {
    if (!submittingReply) {
      if (replyEl?.value) replyStorage.set(replyEl.value);
      else replyStorage.remove();
    }
  });

  $('form.reply').on('submit', () => {
    replyStorage.remove();
    submittingReply = true;
  });
  if (replyEl?.value) replyEl.scrollIntoView(); // scrollto if pre-populated
});
