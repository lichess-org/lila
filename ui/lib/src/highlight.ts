/* Ported to typescript from https://github.com/marmelab/highlight-search-term/blob/main/src/index.js */

export const highlightSearchTerm = (search: string, selector: string): void => {
  const highlightName = 'lichess-highlight';

  if (!CSS.highlights) return; // disable feature on Firefox as it does not support CSS Custom Highlight API

  // remove previous highlight
  CSS.highlights.delete(highlightName);
  if (!search) return; // nothing to highlight
  // find all text nodes containing the search term
  const ranges: AbstractRange[] = [];
  try {
    const elements = document.querySelectorAll<HTMLElement>(selector);
    Array.from(elements).map(element => {
      getTextNodesInElementContainingText(element, search).forEach(node => {
        node.parentElement && ranges.push(...getRangesForSearchTermInElement(node.parentElement, search));
      });
    });
  } catch (error) {
    console.error(error);
  }
  if (ranges.length === 0) return;
  // create a CSS highlight that can be styled with the ::highlight(search) pseudo-element

  if (typeof Highlight !== 'function') throw 'no Highlight support';
  const highlight = new Highlight(...ranges);
  CSS.highlights.set(highlightName, highlight);
};

const getTextNodesInElementContainingText = (element: HTMLElement, text: string) => {
  const lowerCaseText = text.toLowerCase();
  const nodes = [];
  const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);
  let node;
  while ((node = walker.nextNode())) {
    if (node.textContent?.toLowerCase().includes(lowerCaseText)) nodes.push(node);
  }
  return nodes;
};

const getRangesForSearchTermInElement = (element: HTMLElement, search: string) => {
  const ranges: AbstractRange[] = [];
  const lowerCaseSearch = search.toLowerCase();
  if (element.childNodes.length === 0) return ranges;
  // In some frameworks like React, when combining static text with dynamic text, the element may have multiple Text child nodes.
  // To avoid errors, we must find the child node that actually contains the search term.
  const childWithSearchTerm = Array.from(element.childNodes).find(node =>
    node.textContent?.toLowerCase().includes(lowerCaseSearch),
  );
  if (!childWithSearchTerm) return ranges;
  const text = childWithSearchTerm.textContent?.toLowerCase() || '';
  let start = 0;
  let index;
  while ((index = text.indexOf(lowerCaseSearch, start)) >= 0) {
    const range = new Range();
    range.setStart(childWithSearchTerm, index);
    range.setEnd(childWithSearchTerm, index + search.length);
    ranges.push(range);
    start = index + search.length;
  }
  return ranges;
};
