export const wikiBooksUrl = 'https://en.wikibooks.org';
export const apiArgs = 'redirects&origin=*&action=query&prop=extracts&formatversion=2&format=json&stable=1';

const removeH1 = (html: string) => html.replace(/<h1.+<\/h1>/g, '');
const removeEmptyParagraph = (html: string) => html.replace(/<p>(<br \/>|\s)*<\/p>/g, '');
const removeTheoryTableSection = (html: string) =>
  html.replace(/<h2 data-mw-anchor="Theory_table">Theory table<\/h2>.*?(?=<h[1-6]|$)/gs, '');

const removeAllBlacksMovesSection = (html: string) =>
  html.replace(
    /<h2 data-mw-anchor="All_possible_Black's_moves" data-mw-fallback-anchor="All_possible_Black\.27s_moves">All possible Black's moves<\/h2>.*?(?=<h[1-6]|$)/gs,
    '',
  );

const removeAllPossibleRepliesSection = (html: string) =>
  html.replace(/<h3 data-mw-anchor="All_possible_replies">All possible replies<\/h3>.*?(?=<h[1-6]|$)/gs, '');

const removeExternalLinksSection = (html: string) =>
  html.replace(/<h2 data-mw-anchor="External_links">External links<\/h2>.*?(?=<h[1-6]|$)/gs, '');
const removeContributing = (html: string) =>
  html.replace('When contributing to this Wikibook, please follow the Conventions for organization.', '');

const readMore = (title: string) =>
  `<p><a target="_blank" href="${wikiBooksUrl}/wiki/${title}">Read more on WikiBooks</a></p>`;

export const transformWikiHtml = (html: string, title: string): string =>
  removeH1(
    removeEmptyParagraph(
      removeTheoryTableSection(
        removeAllBlacksMovesSection(
          removeAllPossibleRepliesSection(removeExternalLinksSection(removeContributing(html))),
        ),
      ),
    ),
  ) + readMore(title);
