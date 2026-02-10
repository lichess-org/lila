import * as xhr from 'lib/xhr';
import { domDialog } from 'lib/view';
import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';
import { tempStorage } from 'lib/storage';
import { setMode } from './markdownTextarea';

site.load.then(() => {
  $('.forum')
    .on('click', 'a.delete', function (this: HTMLAnchorElement) {
      const link = this;
      domDialog({
        cash: $('.forum-delete-modal'),
        attrs: { view: { action: link.href } },
        modal: true,
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
        dlg.show();
      });
      return false;
    })
    .on('click', 'a.mod-relocate', function (this: HTMLAnchorElement) {
      const link = this;
      domDialog({
        cash: $('.forum-relocate-modal'),
        attrs: { view: { action: link.href } },
        modal: true,
      }).then(dlg => {
        $(dlg.view).find('form').attr('action', link.href);
        $(dlg.view).find('form button.cancel').on('click', dlg.close);
        dlg.show();
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

      const post = this.closest('.forum-post')!;
      const form = post.querySelector<HTMLFormElement>('form.edit-post-form')!;
      if (!form.classList.contains('none')) {
        form.classList.add('none');
        form.reset();
        return;
      }
      const textarea = post.querySelector<HTMLTextAreaElement>('textarea.edit-post-box')!;
      textarea.value = post.querySelector('.forum-post__message-source')!.textContent;
      form.classList.remove('none');
      setMode(textarea, 'write');
    });

  const quoted = new Set<string>();

  $('.quote.button').on('click', function (this: HTMLButtonElement) {
    const post = this.closest('.forum-post')!,
      authorUsername = $(post).find('.author').attr('href')?.substring(3),
      author = authorUsername ? '@' + authorUsername : $(post).find('.author').text(),
      reply = document.querySelector<HTMLTextAreaElement>('.reply .post-text-area')!;

    const lines = (
      quotedMarkdown(this.closest('article')) ??
      post.querySelector('.forum-post__message-source')!.textContent
    ).split('\n');
    if (lines[0].match(/^(?:> )*@.+ said in #\d+:$/)) lines.shift();

    if (lines.length === 0) return;

    const quote =
      `${author} said:\n` +
      lines
        .map(line => `> ${line}\n`)
        .join('')
        .trim() +
      '\n\n';

    if (quoted.has(quote)) return;
    quoted.add(quote);
    setMode(reply, 'write');
    reply.value = reply.value.slice(0, reply.selectionStart) + quote + reply.value.slice(reply.selectionEnd);
    const caretOffset = reply.selectionStart + quote.length;
    reply.setSelectionRange(caretOffset, caretOffset);
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
        match: /(^|\s)@([a-zA-Z_-][\w-]{0,19})$/,
        search: function (term: string, callback: (names: string[]) => void) {
          // Initially we only autocomplete by participants in the thread. As the user types more,
          // we can autocomplete against all users on the site.
          threadParticipants.then(function (participants) {
            const forumParticipantCandidates = searchCandidates(term, participants);

            if (forumParticipantCandidates.length !== 0) {
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

  const replyStorage = tempStorage.make('forum.reply' + location.pathname);
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
    if (submittingReply) return false;
    replyStorage.remove();
    submittingReply = true;
  });
  if (replyEl?.value) replyEl.scrollIntoView(); // scrollto if pre-populated
});

function quotedMarkdown(postEl: HTMLElement | null): string | undefined {
  const selection = window.getSelection();
  if (!postEl || !selection || selection.rangeCount === 0) return undefined;

  const r = selection.getRangeAt(0);
  if (!postEl?.contains(r.startContainer) || !postEl.contains(r.endContainer)) return undefined;

  const startEl =
    r.startContainer.nodeType === 3 ? r.startContainer.parentElement : (r.startContainer as Element);
  const endEl = r.endContainer.nodeType === 3 ? r.endContainer.parentElement : (r.endContainer as Element);

  const startCap = Number(startEl?.closest<HTMLElement>('[data-ms]')?.dataset.ms);
  const endCap = Number(endEl?.closest<HTMLElement>('[data-me]')?.dataset.me);
  const source = postEl.querySelector('.forum-post__message-source')?.textContent;

  if (isNaN(startCap) || isNaN(endCap) || !source) return undefined;

  const sourceLines = selection.toString().trim().split('\n');
  const lastLine = sourceLines[sourceLines.length - 1].trim();

  const startSource = source.indexOf(sourceLines[0].trim(), startCap);
  const endSource = source.lastIndexOf(lastLine, endCap) + lastLine.length;

  return prefixQuote(source, startCap) + source.slice(startSource, endSource);
}

function prefixQuote(text: string, offset: number) {
  let prefix = '';
  while (offset-- > 1) {
    const char = text.slice(offset, offset + 1);
    if (char === '\n') break;
    else if (char === '>') prefix += '> ';
    else if (char.trim().length) return '';
  }
  return prefix;
}
