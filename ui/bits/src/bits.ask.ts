import * as xhr from 'common/xhr';
import { throttle } from 'common/timing';
import { isTouchDevice } from 'common/device';

export default function initModule(): void {
  $('.ask-container').each((_, e: EleLoose) => new Ask(e.firstElementChild!));
}

if (isTouchDevice()) site.asset.loadIife('javascripts/vendor/dragdroptouch.js');

class Ask {
  el: Element;
  anon: boolean;
  submitEl?: Element;
  formEl?: HTMLInputElement;
  view: string; // the initial order of picks when 'random' tag is used
  initialRanks: string; // initial rank order
  initialForm: string; // initial form value
  db: 'clean' | 'hasPicks'; // clean means no picks for this (ask, user) in the db
  constructor(askEl: Element) {
    this.el = askEl;
    this.anon = askEl.classList.contains('anon');
    this.db = askEl.hasAttribute('value') ? 'hasPicks' : 'clean';
    this.view = Array.from($('.choice', this.el), e => e?.getAttribute('value')).join('-');
    this.initialRanks = this.ranking();
    this.initialForm = this.formEl?.value ?? '';
    wireSubmit(this);
    wireForm(this);
    wireRankedChoices(this);
    wireExclusiveChoices(this);
    wireMultipleChoices(this);
    wireActions(this);
  }

  ranking(): string {
    return Array.from($('.choice.rank', this.el), e => e?.getAttribute('value')).join('-');
  }
  relabel() {
    const submitted = this.ranking() == this.initialRanks && this.db == 'hasPicks';
    $('.choice.rank', this.el).each((i, e) => {
      $('div', e).text(`${i + 1}`);
      e.classList.toggle('submitted', submitted);
    });
  }
  setSubmitState(state: 'clean' | 'dirty' | 'success') {
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
  return xhr.textRaw(req.url, { method: req.method ? req.method : 'POST', body: req.body }).then(
    async(rsp: Response) => {
      if (rsp.redirected) {
        if (!rsp.url.startsWith(window.location.origin)) throw new Error(`Bad redirect: ${rsp.url}`);
        window.location.href = rsp.url;
        return;
      }
      const newAsk = rewire(req.ask.el, await xhr.ensureOk(rsp).text());
      if (req.after) req.after(newAsk!);
    },
    (rsp: Response) => {
      console.log(`Ask failed: ${rsp.status} ${rsp.statusText}`);
    },
  );
}

function wireSubmit(ask: Ask) {
  ask.submitEl = $('.form-submit', ask.el).get(0);
  if (!ask.submitEl) return;
  $('input', ask.submitEl).on('click', async() => {
    const path = `/ask/form/${ask.el.id}?view=${ask.view}&anon=${ask.el.classList.contains('anon')}`;
    const body = ask.formEl?.value ? xhr.form({ text: ask.formEl.value }) : undefined;
    const newOrder = ask.ranking();
    if (newOrder && (ask.db === 'clean' || newOrder != ask.initialRanks))
      await askXhr({
        ask: ask,
        url: ask.picksUrl(newOrder),
        body: body,
        after: ask => ask.setSubmitState('success'),
      });
    else if (ask.formEl)
      askXhr({
        ask: ask,
        url: path,
        body: body,
        after: ask => ask.setSubmitState(ask.formEl?.value ? 'success' : 'clean'),
      });
  });
}

function wireExclusiveChoices(ask: Ask) {
  $('.choice.exclusive', ask.el).on('click', function(e: Event) {
    const el = e.target as Element;
    askXhr({
      ask: ask,
      url: ask.picksUrl(el.classList.contains('selected') ? '' : el.getAttribute('value')!),
    });
    e.preventDefault();
  });
}

function wireMultipleChoices(ask: Ask) {
  $('.choice.multiple', ask.el).on('click', function(e: Event) {
    $(e.target as Element).toggleClass('selected');
    const picks = $('.choice', ask.el)
      .filter((_, x) => x.classList.contains('selected'))
      .get()
      .map(x => x.getAttribute('value'));
    askXhr({ ask: ask, url: ask.picksUrl(picks.join('-')) });
    e.preventDefault();
  });
}

function wireForm(ask: Ask) {
  ask.formEl = $('.form-text', ask.el)
    .on('input', () => {
      const dirty =
        ask.formEl?.value != ask.initialForm ||
        (ask.initialRanks && (ask.ranking() != ask.initialRanks || ask.db === 'clean'));
      ask.setSubmitState(dirty ? 'dirty' : 'clean');
    })
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
}

function wireActions(ask: Ask) {
  $('.url-actions button', ask.el).on('click', (e: Event) => {
    const btn = e.target as HTMLButtonElement;
    askXhr({ ask: ask, method: btn.formMethod, url: btn.formAction });
  });
}

function wireRankedChoices(ask: Ask) {
  let d: DragContext;

  const container = $('.ask__choices', ask.el);
  const vertical = container.hasClass('vertical');
  const [cursorEl, breakEl] = createCursor(vertical);
  const updateCursor = throttle(100, (d: DragContext, e: DragEvent) => {
    // avoid processing a delayed drag event after the drop
    const ePoint = { x: e.clientX, y: e.clientY };
    if (!d.isDone) vertical ? updateVCursor(d, ePoint) : updateHCursor(d, ePoint);
  });

  if (ask.db === 'clean') ask.setSubmitState('dirty');
  container.on('dragover dragleave', (e: DragEvent) => {
    e.preventDefault();
    updateCursor(d, e);
  });
  /*.on('dragleave', (e: DragEvent) => {
      e.preventDefault();
      updateCursor(d, e);
    });*/

  $('.choice.rank', ask.el) // wire each draggable
    .on('dragstart', (e: DragEvent) => {
      e.dataTransfer!.effectAllowed = 'move';
      e.dataTransfer!.setData('text/plain', ''); //$('label', e.target as Element).text());
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
      ask.relabel();
      if (ask.ranking() != ask.initialRanks) ask.setSubmitState('dirty');
      /*const newOrder = ask.ranking();
      if (newOrder == ask.initialRanks) return;
      askXhr({
        ask: ask,
        url: ask.picksUrl(newOrder),
        after: () => {
          ask.initialOrder = newOrder;
        },
      });*/
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

function clearCursor(d: DragContext) {
  if (d.cursorEl.parentNode) d.parentEl.removeChild(d.cursorEl);
  if (d.breakEl?.parentNode) d.parentEl.removeChild(d.breakEl);
}

function updateHCursor(d: DragContext, e: { x: number; y: number }) {
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

function updateVCursor(d: DragContext, e: { x: number; y: number }) {
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
