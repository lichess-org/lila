import { attributesModule, classModule, init, type VNode } from 'snabbdom';

import { hl } from 'lib/view';
import { jsonSimple } from 'lib/xhr';

const patch = init([classModule, attributesModule]);
const dateTime = new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'UTC' });

interface Result {
  date: number;
  result: 'success' | 'failed' | 'cleared';
  error?: string;
}

interface Transaction {
  jobType: string;
  model: string;
  source: Source;
  updated: number;
  response?: Result;
}

interface Source {
  id?: string;
  name?: string;
  url?: string;
}

interface JobHealth {
  jobType: string;
  model: string;
  inputTokens: number;
  outputTokens: number;
  recent: number;
  failures: number;
}

interface AutomodStatus {
  recentJobs: Transaction[];
  pending: number;
  jobsByTypeAndModel: JobHealth[];
  adminLinks?: Record<string, string>;
}

const failed = (transaction: Transaction) => transaction.response?.result === 'failed';

function view(status: AutomodStatus): VNode {
  const lastSuccess = status.recentJobs.find(transaction => transaction.response && !failed(transaction));
  const lastFailure = status.recentJobs.find(failed);
  return hl('div.box.box-pad.page-small.automod-status', [
    hl('fieldset', [
      hl('legend', 'Automod status'),
      hl('div.grid', [
        hl(`p.${lastFailure ? 'yellow' : 'green'}`, [
          hl('strong', status.recentJobs.filter(failed).length),
          ' failed / ',
          hl('strong', status.recentJobs.length),
          ' recent',
        ]),
        hl('p', [
          lastSuccess && ['Last success: ', dateTime.format(lastSuccess.response!.date), hl('br')],
          lastFailure &&
            hl('span', { attrs: { title: lastFailure.response?.error ?? '' } }, [
              'Last failed: ',
              dateTime.format(lastFailure.response!.date),
            ]),
        ]),
        hl('p.pending', `${status.pending} pending`),
      ]),
    ]),
    status.jobsByTypeAndModel.length > 0 &&
      hl('fieldset', [
        hl('legend', 'Job types'),
        hl(
          'div.grid',
          status.jobsByTypeAndModel
            .sort(
              (left, right) =>
                left.jobType.localeCompare(right.jobType) || left.model.localeCompare(right.model),
            )
            .flatMap(job => [
              status.adminLinks?.[job.jobType]
                ? hl('a.admin', { attrs: { href: status.adminLinks[job.jobType] } }, job.jobType)
                : hl('p', job.jobType),
              hl('p', `${job.model}: ${job.inputTokens} input / ${job.outputTokens} output`),
              hl(`p.${job.failures ? 'yellow' : 'green'}`, `${job.failures} failed / ${job.recent} recent`),
            ]),
        ),
      ]),
    status.recentJobs.length > 0 &&
      hl('fieldset', [
        hl('legend', 'Recent jobs'),
        hl('table.slist', hl('tbody', status.recentJobs.map(transactionRow))),
      ]),
  ]);
}

function transactionRow(transaction: Transaction): VNode {
  const { id, name, url } = transaction.source;
  const label = name ?? id ?? url ?? '';
  const title = name && id ? id : undefined;
  const source = url
    ? hl('a', title ? { attrs: { href: url, title } } : { attrs: { href: url } }, label)
    : hl('span', title ? { attrs: { title } } : {}, label);
  return hl('tr', [
    hl('td', transaction.jobType),
    hl('td.source', [
      source,
      transaction.response?.error && hl('span', { attrs: { title: transaction.response.error } }, ' failed'),
    ]),
    hl('td', transaction.model),
    hl(
      'td',
      transaction.response
        ? dateTime.format(transaction.response.date)
        : dateTime.format(transaction.updated),
    ),
    hl(
      'td' +
        (transaction.response?.result === 'success'
          ? '.green'
          : transaction.response?.result === 'failed'
            ? '.red'
            : '.yellow'),
      transaction.response?.result ?? 'pending',
    ),
  ]);
}

export function initModule(data: AutomodStatus): void {
  const el = document.querySelector<HTMLElement>('.automod-status');
  const url = el?.dataset.url;
  if (!url) return;
  let vnode = patch(el, view(data));

  const update = async () => {
    try {
      vnode = patch(vnode, view(await jsonSimple(url)));
    } catch {}
  };
  update();
  setInterval(update, 3000);
}
