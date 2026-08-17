import { attributesModule, classModule, init, type VNode } from 'snabbdom';

import { displayColumns } from 'lib/device';
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

function view(status: AutomodStatus): VNode {
  const failed = (transaction: Transaction) => transaction.response?.result === 'failed';
  const lastSuccess = status.recentJobs.find(transaction => transaction.response && !failed(transaction));
  const lastFailure = status.recentJobs.find(failed);
  const recentFails = status.recentJobs.filter(failed).length;
  const inputPercentage =
    status.recentJobs.length === 0 ? 0 : Math.round((recentFails / status.recentJobs.length) * 100);
  return hl('div.box.box-pad.page-small.automod-status', [
    hl('fieldset', [
      hl('legend', 'Automod status'),
      hl('div.grid', [
        hl(
          `p.proportion.fail.${lastFailure ? 'yellow' : 'green'}`,
          { attrs: { style: `--input-percentage:${inputPercentage}%` } },
          [hl('strong', recentFails), ' failed / ', hl('strong', status.recentJobs.length), ' recent'],
        ),
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
        hl('legend', ['Model usage ', hl('span', '(in past 30 days)')]),
        hl('div.grid', [
          displayColumns() > 1 && [
            hl('label'),
            hl('label', ['tokens (', hl('span.dim', 'input'), ' / ', hl('span.dim', 'output'), ' / total)']),
            hl('label', 'failures'),
          ],
          status.jobsByTypeAndModel
            .sort(
              (left, right) =>
                left.jobType.localeCompare(right.jobType) || left.model.localeCompare(right.model),
            )
            .flatMap(job => [
              status.adminLinks?.[job.jobType]
                ? hl('a.admin', { attrs: { href: status.adminLinks[job.jobType] } }, job.jobType)
                : hl('p', job.jobType),
              usageTokens(job),
              past30days(job),
            ]),
        ]),
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

function usageTokens(job: JobHealth): VNode {
  const [input, output, total] = [job.inputTokens, job.outputTokens, job.inputTokens + job.outputTokens];
  const inputPercentage = total === 0 ? 0 : Math.round((100 * input) / total);
  const friendly = (n: number) =>
    n > 1_000_000
      ? `${Math.round(n / 100_000) / 10}M`
      : n > 1000
        ? `${Math.round(n / 100) / 10}k`
        : n.toString();
  return hl('p.proportion', { attrs: { style: `--input-percentage:${inputPercentage}%;` } }, [
    `${job.model} `,
    hl('span.dim', friendly(input)),
    ' / ',
    hl('span.dim', friendly(output)),
    ' / ',
    friendly(total),
  ]);
}

function past30days(job: JobHealth): VNode {
  const failures = job.failures;
  const recent = job.recent;
  const percentage = recent === 0 ? 0 : Math.round((100 * failures) / recent);
  const color = job.failures ? 'yellow' : 'green';
  return hl(`p.proportion.fail.${color}`, { attrs: { style: `--input-percentage:${percentage}%;` } }, [
    `${failures} failed / ${recent} total`,
  ]);
}
