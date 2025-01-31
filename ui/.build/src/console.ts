import { createServer, IncomingMessage, ServerResponse } from 'node:http';
import { env, errorMark, warnMark, c } from './env.ts';

export async function startConsole() {
  if (!env.remoteLog || !env.watch) return;
  createServer((req: IncomingMessage, res: ServerResponse) => {
    if (req.method === 'OPTIONS')
      return res
        .writeHead(200, '', {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'POST',
          'Access-Control-Allow-Private-Network': 'true',
        })
        .end();
    if (req.method !== 'POST') return res.writeHead(404).end();

    let body = '';

    req.on('data', chunk => (body += chunk.toString()));
    req.on('end', () => {
      try {
        let [[level, val]] = Object.entries<any>(JSON.parse(body));
        const mark = level === 'error' ? `${errorMark} - ` : level === 'warn' ? `${warnMark} - ` : '';
        if (!Array.isArray(val)) return;
        if (val.length <= 1) val = val[0] ?? '';
        else if (val.every(x => typeof x === 'string')) val = val.join(' ');

        env.log(`${mark}${c.grey(typeof val === 'string' ? val : JSON.stringify(val, undefined, 2))}`, 'web');
        res
          .writeHead(200, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'POST' })
          .end();
      } catch (_) {
        res.writeHead(400).end();
      }
    });
  }).listen(8666);
}

export function jsLogger(): string {
  const logUrl = typeof env.remoteLog === 'string' ? env.remoteLog : 'http://localhost:8666';
  return (
    `(function(){const o={log:console.log,info:console.info,warn:console.warn,error:console.error},` +
    `l=["log","info","warn","error"];for(const s of l)console[s]=(...a)=>r(s,...a);async function ` +
    `r(s,...a){o[s](...a);if(await fetch("${logUrl}",{method:"POST",body:JSON.stringify({[s]:a})})` +
    `.then(e=>!e.ok).catch(()=>true))for(const s of l)console[s]=o[s];}})();`
  );
}
