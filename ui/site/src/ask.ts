import * as xhr from 'common/xhr';
import throttle from 'common/throttle';

export default function initModule() {
  lichess.load.then(() => $('.ask-container').each((_, e: EleLoose) => new Ask(e.firstElementChild!)));
}

class Ask {
  el: Element;
  anon: boolean;
  submitEl?: Element;
  feedbackEl?: HTMLInputElement;
  view: string; // the initial order of picks when 'random' tag is used
  db: 'clean' | 'hasPicks'; // clean means no picks for this (ask, user) in the db
  constructor(askEl: Element) {
    this.el = askEl;
    this.anon = askEl.classList.contains('anon');
    this.db = askEl.hasAttribute('value') ? 'hasPicks' : 'clean';
    this.view = Array.from($('.choice', this.el), e => e?.getAttribute('value')).join('-');
    wireExclusiveChoices(this);
    wireMultipleChoices(this);
    wireRankedChoices(this);
    wireActions(this);
    wireFeedback(this);
    wireSubmit(this);
  }
  ranking(): string {
    return Array.from($('.choice.rank', this.el), e => e?.getAttribute('value')).join('-');
  }
  feedbackState(state: 'clean' | 'dirty' | 'success') {
    this.submitEl?.classList.remove('dirty', 'success');
    if (state != 'clean') this.submitEl?.classList.add(state);
  }
  picksUrl(picks: string): string {
    return `/ask/picks/${this.el.id}${picks ? `?picks=${picks}&` : '?'}view=${this.view}${
      this.el.classList.contains('anon') ? '&anon=true' : ''
    }`;
  }
}

function rewire(el: Element | null, frag: string): Ask | undefined {
  while (el && !el.classList.contains('ask-container')) el = el.parentElement;
  if (el && frag) {
    el.innerHTML = frag;
    return new Ask(el.firstElementChild!);
  }
}

function askXhr(req: { ask: Ask; url: string; method?: string; body?: FormData; after?: (_: Ask) => void }) {
  xhr.textRaw(req.url, { method: req.method ? req.method : 'POST', body: req.body }).then(
    async (rsp: Response) => {
      if (rsp.redirected) {
        if (!rsp.url.startsWith(window.location.origin)) throw new Error(`Bad redirect: ${rsp.url}`);
        window.location.href = rsp.url;
        return;
      }
      console.log(req.url);
      const newAsk = rewire(req.ask.el, await xhr.ensureOk(rsp).text());
      if (req.after) req.after(newAsk!);
    },
    (rsp: Response) => {
      console.log(`Ask failed: ${rsp.status} ${rsp.statusText}`);
    },
  );
}

function wireExclusiveChoices(ask: Ask) {
  $('.choice.exclusive', ask.el).on('click', function (e: Event) {
    const el = e.target as Element;
    askXhr({
      ask: ask,
      url: ask.picksUrl(el.classList.contains('selected') ? '' : el.getAttribute('value')!),
    });
    e.preventDefault();
  });
}

function wireMultipleChoices(ask: Ask) {
  $('.choice.multiple', ask.el).on('click', function (e: Event) {
    $(e.target as Element).toggleClass('selected');
    const picks = $('.choice', ask.el)
      .filter((_, x) => x.classList.contains('selected'))
      .get()
      .map(x => x.getAttribute('value'));
    askXhr({ ask: ask, url: ask.picksUrl(picks.join('-')) });
    e.preventDefault();
  });
}

function wireFeedback(ask: Ask) {
  ask.feedbackEl = $('.form-text', ask.el)
    .on('input', () => ask.feedbackState(ask.feedbackEl?.value == initialFeedback ? 'clean' : 'dirty'))
    .on('keypress', (e: KeyboardEvent) => {
      if (
        e.key != 'Enter' ||
        e.shiftKey ||
        e.ctrlKey ||
        e.altKey ||
        e.metaKey ||
        !ask.submitEl?.classList.contains('dirty')
      )
        return;
      $('input', ask.submitEl).trigger('click');
      e.preventDefault();
    })
    .get(0) as HTMLInputElement;
  const initialFeedback = ask.feedbackEl?.value;
}

function wireSubmit(ask: Ask) {
  ask.submitEl = $('.form-submit', ask.el).get(0);
  if (!ask.submitEl) return;
  $('input', ask.submitEl).on('click', () => {
    const path = `/ask/form/${ask.el.id}?view=${ask.view}&anon=${ask.el.classList.contains('anon')}`;
    const body = ask.feedbackEl?.value ? xhr.form({ text: ask.feedbackEl.value }) : undefined;
    askXhr({
      ask: ask,
      url: path,
      body: body,
      after: ask => ask.feedbackState(ask.feedbackEl?.value ? 'success' : 'clean'),
    });
  });
}

function wireActions(ask: Ask) {
  $('.url-actions button', ask.el).on('click', (e: Event) => {
    const btn = e.target as HTMLButtonElement;
    askXhr({ ask: ask, method: btn.formMethod, url: btn.formAction });
  });
}

function wireRankedChoices(ask: Ask): void {
  let initialOrder = ask.ranking();
  let d: DragContext;

  const container = $('.ask__choices', ask.el);
  const vertical = container.hasClass('vertical');
  const [cursorEl, breakEl] = createCursor(vertical);
  const updateCursor = throttle(100, (d: DragContext, e: DragEvent) => {
    // avoid processing a delayed drag event after the drop
    if (!d.isDone) vertical ? updateVCursor(d, e) : updateHCursor(d, e);
  });

  container
    .on('dragover', (e: DragEvent) => {
      e.preventDefault();
      updateCursor(d, e);
    })
    .on('dragleave', (e: DragEvent) => {
      e.preventDefault();
      updateCursor(d, e);
    });

  $('.choice.rank', ask.el) // wire each draggable
    .on('dragstart', (e: DragEvent) => {
      e.dataTransfer!.effectAllowed = 'move';
      e.dataTransfer!.setData('text/plain', '');
      const dragEl = e.target as Element;
      dragEl.classList.add('dragging');
      d = {
        dragEl: dragEl,
        parentEl: dragEl.parentElement!,
        box: dragEl.parentElement!.getBoundingClientRect(),
        cursorEl: cursorEl!,
        breakEl: breakEl,
        choices: Array.from($('.choice.rank', ask.el), e => e!),
        isDone: false,
      };
    })
    .on('dragend', (e: DragEvent) => {
      e.preventDefault();
      d.isDone = true;
      d.dragEl.classList.remove('dragging');
      if (d.cursorEl.parentElement != d.parentEl) return;
      d.parentEl.insertBefore(d.dragEl, d.cursorEl);
      clearCursor(d);

      const newOrder = ask.ranking();
      if (newOrder == initialOrder) return;
      askXhr({
        ask: ask,
        url: ask.picksUrl(newOrder),
        after: () => {
          initialOrder = newOrder;
        },
      });
    });
}

type DragContext = {
  dragEl: Element; // we are dragging this
  parentEl: Element; // the div.ask__chioces containing the draggables
  box: DOMRect; // the rectangle containing all draggables
  cursorEl: Element; // the insertion cursor (I beam div or <hr> depending on mode)
  breakEl: Element | null; // null if vertical, a div {flex-basis: 100%} if horizontal
  choices: Array<Element>; // the draggable elements
  isDone: boolean; // emerge victorious after the onslaught of throttled dragover events
  data?: any; // used to track dirty state in updateHCursor
};

function createCursor(vertical: boolean) {
  if (vertical) return [document.createElement('hr'), null];

  const cursorEl = document.createElement('div');
  cursorEl.classList.add('cursor');
  const breakEl = document.createElement('div');
  breakEl.style.flexBasis = '100%';
  return [cursorEl, breakEl];
}

function clearCursor(d: DragContext): void {
  if (d.cursorEl.parentNode) d.parentEl.removeChild(d.cursorEl);
  if (d.breakEl?.parentNode) d.parentEl.removeChild(d.breakEl);
}

function updateHCursor(d: DragContext, e: MouseEvent): void {
  if (e.x <= d.box.left || e.x >= d.box.right || e.y <= d.box.top || e.y >= d.box.bottom) {
    clearCursor(d);
    d.data = null;
    return;
  }
  const rtl = document.dir == 'rtl';
  let target: { el: Element | null; break: 'beforebegin' | 'afterend' | null } | null = null;
  for (let i = 0, lastY = 0; i < d.choices.length && !target; i++) {
    const r = d.choices[i].getBoundingClientRect();
    const x = r.right - r.width / 2;
    const y = r.bottom + 4; // +4 because there's (currently) 8 device px between rows
    const rowBreak = i > 0 && y != lastY;
    if (rowBreak && e.y <= lastY) target = { el: d.choices[i], break: 'afterend' };
    else if (e.y <= y && (rtl ? e.x >= x : e.x <= x))
      target = { el: d.choices[i], break: rowBreak ? 'beforebegin' : null };
    lastY = y;
  }
  if (d.data && target && d.data.el == target.el && d.data.break == target.break) return; // nothing to do here

  d.data = target; // keep last target in context data so we only diddle the DOM when dirty

  if (!target) {
    d.parentEl.insertBefore(d.cursorEl, null);
    return;
  }
  d.parentEl.insertBefore(d.cursorEl, target.el);
  if (target.break) {
    // don't add break when inserting the cursor at the end of a line with no room
    if (target.break != 'afterend' || d.cursorEl.getBoundingClientRect().top < e.y)
      d.cursorEl.insertAdjacentElement(target.break, d.breakEl!);
  } else if (d.breakEl!.parentNode) d.parentEl.removeChild(d.breakEl!);
}

function updateVCursor(d: DragContext, e: DragEvent): void {
  if (e.x <= d.box.left || e.x >= d.box.right || e.y <= d.box.top || e.y >= d.box.bottom) {
    clearCursor(d);
    return;
  }
  let target: Element | null = null;
  for (let i = 0; i < d.choices.length && !target; i++) {
    const r = d.choices[i].getBoundingClientRect();
    if (e.y < r.top + r.height / 2) target = d.choices[i];
  }
  d.parentEl.insertBefore(d.cursorEl, target);
}
