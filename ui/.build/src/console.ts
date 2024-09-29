import { createServer, IncomingMessage, ServerResponse } from 'node:http';
import { env, errorMark, warnMark, colors as c } from './main';

export async function startConsole() {
  if (!env.debug || !env.watch) return;
  createServer((req: IncomingMessage, res: ServerResponse) => {
    if (req.method === 'OPTIONS')
      return res
        .writeHead(200, '', {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'POST',
          'Access-Control-Allow-Private-Network': 'true',
        })
        .end();
    if (req.method !== 'POST' || req.url !== '/debug') return res.writeHead(404).end();

    let body = '';

    req.on('data', chunk => (body += chunk.toString()));
    req.on('end', () => {
      try {
        let [[level, val]] = Object.entries<any>(JSON.parse(body));
        const mark = level === 'error' ? `${errorMark} - ` : level === 'warn' ? `${warnMark} - ` : '';
        if (!Array.isArray(val)) return;
        if (val.length <= 1) val = val[0] ?? '';
        else if (val.every(x => typeof x === 'string')) val = val.join(' ');

        env.log(`${mark}${c.grey(typeof val === 'string' ? val : JSON.stringify(val, undefined, 2))}`, {
          ctx: 'web',
        });
        res
          .writeHead(200, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'POST' })
          .end();
      } catch (_) {
        res.writeHead(400).end();
      }
    });
  }).listen(8666);
}
