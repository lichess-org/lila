import { isTouchDevice } from 'lib/device';
import { textRaw as xhrTextRaw, form as xhrForm, ensureOk } from 'lib/xhr';

export default function initModule(): void {
  // normal ui
  for (const askContainer of document.querySelectorAll<HTMLElement>('.ask-container')) {
    new Ask(askContainer.firstElementChild as HTMLElement);
  }
  // admin ui
  for (const adminButton of document.querySelectorAll<HTMLButtonElement>(
    '.ask-admin .url-actions button, .ask-group-header .url-actions button',
  )) {
    if (adminButton.closest('.ask-container')) continue;
    adminButton.onclick = async () => {
      await xhrTextRaw(adminButton.formAction, { method: 'POST' });
      site.reload();
    };
  }
}

const enableDragDropTouch = isTouchDevice()
  ? import(site.asset.url('npm/drag-drop-touch.esm.min.js')).then(m => m.enableDragDropTouch)
  : Promise.resolve(() => {});

class Ask {
  anon: boolean;
  submitEl?: Element;
  formEl?: HTMLInputElement;
  viewOrder: string; // the initial order of picks when 'random' tag is used
  initialRanks: string; // initial rank order
  initialFormValue: string; // initial form value
  hasPick: boolean; // whether there are picks/form data for this (ask, user) in the db

  constructor(readonly el: HTMLElement) {
    this.anon = this.el.classList.contains('anon');
    this.hasPick = this.el.dataset.hasPick === 'true';
    this.viewOrder = Array.from($('.choice', this.el), e => e?.getAttribute('value')).join('-');
    this.initialRanks = this.ranking();
    wireSubmit(this);
    wireForm(this);
    wireRankedChoices(this);
    wireExclusiveChoices(this);
    wireMultipleChoices(this);
    wireActions(this);
  }

  ranking(): string {
    return Array.from(this.el.querySelectorAll('.choice.rank'), el => el?.getAttribute('value')).join('-');
  }

  relabel() {
    const submitted = this.ranking() === this.initialRanks && this.hasPick;
    this.el.querySelectorAll('.choice.rank').forEach((choice, i) => {
      const label = choice.querySelector('div');
      if (label) label.textContent = `${i + 1}`;
      choice.classList.toggle('submitted', submitted);
    });
  }

  setSubmitState(state: 'clean' | 'dirty' | 'success') {
    this.submitEl?.classList.remove('dirty', 'success');
    if (state !== 'clean') this.submitEl?.classList.add(state);
  }

  picksUrl(picks: string): string {
    return `/ask/picks/${this.el.id}${picks ? `?picks=${picks}&` : '?'}view=${this.viewOrder}${
      this.el.classList.contains('anon') ? '&anon=true' : ''
    }`;
  }
}

async function postAsk(req: {
  ask: Ask;
  url: string;
  method?: string;
  body?: FormData;
  replaceAfter?: Promise<void>;
}): Promise<Ask> {
  const rsp = await xhrTextRaw(req.url, { method: req.method || 'POST', body: req.body });
  if (rsp.redirected) {
    if (!rsp.url.startsWith(window.location.origin)) throw new Error(`Bad redirect: ${rsp.url}`);
    window.location.href = rsp.url;
    return req.ask;
  }
  const container = req.ask.el.closest('.ask-container');
  if (!container) return req.ask;

  const html = await ensureOk(rsp).then(rsp => rsp.text());
  await req.replaceAfter;
  container.innerHTML = html;
  return new Ask(container.firstElementChild as HTMLElement);
}

function wireSubmit(ask: Ask) {
  ask.submitEl = ask.el.querySelector('.form-submit') ?? undefined;
  if (!ask.submitEl) return;

  ask.submitEl.querySelector('input')!.onclick = () => {
    if (!ask.formEl) return;
    postAsk({
      ask,
      url: `/ask/form/${ask.el.id}?view=${ask.viewOrder}&anon=${ask.el.classList.contains('anon')}`,
      body: xhrForm({ text: ask.formEl.value }),
    }).then(updated => updated.setSubmitState('success'));
  };
}

function wireExclusiveChoices(ask: Ask) {
  for (const choice of ask.el.querySelectorAll<HTMLElement>('.choice.exclusive')) {
    choice.onclick = e => {
      const el = e.target as Element;
      postAsk({ ask, url: ask.picksUrl(el.classList.contains('selected') ? '' : el.getAttribute('value')!) });
      e.preventDefault();
    };
  }
}

function wireMultipleChoices(ask: Ask) {
  for (const choice of ask.el.querySelectorAll<HTMLElement>('.choice.multiple')) {
    choice.onclick = e => {
      if (!(e.target instanceof HTMLElement)) return;
      e.target.classList.toggle('selected');
      const picks = Array.from(ask.el.querySelectorAll<HTMLElement>('.choice'))
        .filter(x => x.classList.contains('selected'))
        .map(x => x.getAttribute('value'));
      postAsk({ ask, url: ask.picksUrl(picks.join('-')) });
      e.preventDefault();
    };
  }
}

function wireForm(ask: Ask) {
  ask.formEl = ask.el.querySelector<HTMLInputElement>('.form-text')!;
  if (!ask.formEl) return;
  ask.initialFormValue = ask.formEl.defaultValue;
  ask.formEl.oninput = () => {
    const dirty =
      ask.formEl?.value !== ask.initialFormValue ||
      (ask.initialRanks && (ask.ranking() !== ask.initialRanks || !ask.hasPick));
    ask.setSubmitState(dirty ? 'dirty' : 'clean');
  };
  ask.formEl.onkeydown = (e: KeyboardEvent) => {
    if (
      e.key !== 'Enter' ||
      e.shiftKey ||
      e.ctrlKey ||
      e.altKey ||
      e.metaKey ||
      !ask.submitEl?.classList.contains('dirty')
    )
      return;
    ask.submitEl.querySelector('input')!.click();
    e.preventDefault();
  };
}

function wireActions(ask: Ask) {
  for (const button of ask.el.querySelectorAll<HTMLButtonElement>('.url-actions button')) {
    button.onclick = () => postAsk({ ask, method: button.formMethod, url: button.formAction });
  }
}

type DragContext = { dragEl: Element; nextEl: Element | null; finished: Promise<void> };

async function wireRankedChoices(ask: Ask) {
  const container = ask.el.querySelector<HTMLElement>('.ask__choices');
  if (!container) return;

  (await enableDragDropTouch)(ask.el, ask.el, { forceListen: false }); // polyfill phones

  let drag: DragContext | undefined;
  const vertical = container.classList.contains('vertical');

  container.ondragover = container.ondrop = e => {
    e.preventDefault();
    if (!drag) return;

    const dragEl = drag.dragEl;
    const { clientX: x, clientY: y } = e;
    const box = container.getBoundingClientRect();
    let target: Element | null = null;
    if (x > box.left && x < box.right && y > box.top && y < box.bottom + (vertical ? 0 : 4)) {
      const choices = Array.from(container.querySelectorAll('.choice.rank')).filter(el => el !== dragEl);
      if (vertical) {
        target =
          choices.find(el => {
            const rect = el.getBoundingClientRect();
            const isUp = Boolean(el.compareDocumentPosition(dragEl) & Node.DOCUMENT_POSITION_FOLLOWING);
            return y < rect.top + (isUp ? rect.height : 0);
          }) ?? null;
      } else {
        const rtl = document.dir === 'rtl';
        let lastY = 0;
        for (let i = 0; i < choices.length && !target; i++) {
          const rect = choices[i].getBoundingClientRect();
          const isUp = Boolean(choices[i].compareDocumentPosition(dragEl) & Node.DOCUMENT_POSITION_FOLLOWING);
          const choiceEdgeX = isUp ? (rtl ? rect.left : rect.right) : rtl ? rect.right : rect.left;
          const belowChoiceY = rect.bottom + 4;
          const rowBreak = i > 0 && belowChoiceY !== lastY;
          if (rowBreak && y <= lastY) target = choices[i];
          else if (y <= belowChoiceY && (rtl ? x >= choiceEdgeX : x <= choiceEdgeX)) target = choices[i];
          lastY = belowChoiceY;
        }
      }
    } else target = drag.nextEl;

    if (dragEl.nextElementSibling !== target)
      drag.finished = transition(() => {
        container.insertBefore(dragEl, target);
        ask.relabel();
      });
  };

  for (const choice of ask.el.querySelectorAll<HTMLElement>('.choice.rank')) {
    choice.style.viewTransitionName = `ask-${ask.el.id}-choice-${choice.getAttribute('value')}`;
    choice.ondragstart = e => {
      e.dataTransfer!.effectAllowed = 'move';
      e.dataTransfer!.setData('text/plain', '');
      drag = { dragEl: choice, nextEl: choice.nextElementSibling, finished: Promise.resolve() };
      container.classList.add('dragging');
    };
    choice.ondragend = e => {
      e.preventDefault();
      if (!drag || drag.dragEl !== choice) return;

      const dropped = container.contains(document.elementFromPoint(e.clientX, e.clientY));
      container.classList.remove('dragging');
      if (dropped) {
        const finished = drag.finished;
        drag = undefined;
        if (ask.ranking() !== ask.initialRanks || !ask.hasPick)
          postAsk({ ask, url: ask.picksUrl(ask.ranking()), replaceAfter: finished });
        return;
      }

      const nextEl = drag.nextEl;
      drag = undefined;
      transition(() => {
        container.insertBefore(choice, nextEl);
        ask.relabel();
      });
    };
  }
}

function transition(update: () => void): Promise<void> {
  if (!document.startViewTransition) {
    update();
    return Promise.resolve();
  }
  const viewTransition = document.startViewTransition(update);
  viewTransition.ready.catch(() => {});
  return viewTransition.finished.catch(error => {
    if (!(error instanceof DOMException) || error.name !== 'AbortError') throw error;
  });
}
