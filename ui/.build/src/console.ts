import { createServer, IncomingMessage, ServerResponse } from 'node:http';
import { env, errorMark, warnMark, c } from './env.ts';
import { transform } from 'esbuild';
import stringify from 'json-stringify-pretty-compact';

export async function startConsole() {
  if (!env.remoteLog || !env.watch) return;
  createServer((req: IncomingMessage, res: ServerResponse) => {
    const fwdFor = req.headers['x-forwarded-for'];
    const ip = (Array.isArray(fwdFor) ? fwdFor[0] : fwdFor) ?? 'web';
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
        const [levelAndVal] = Object.entries<string>(JSON.parse(body));
        const level = levelAndVal[0];
        let val = levelAndVal[1];
        const mark = level === 'error' ? `${errorMark} ` : level === 'warn' ? `${warnMark} ` : '';

        if (!Array.isArray(val)) throw new Error();
        else if (val.length <= 1) val = val[0] ?? '';
        else if (val.every(x => typeof x !== 'object')) val = val.join(' ');

        if (typeof val !== 'string') val = stringify(val, { indent: 2, maxLength: 80 });

        env.log(`${mark}${c.grey(val)}`, ip);
        res
          .writeHead(200, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Methods': 'POST' })
          .end();
      } catch (_) {
        res.writeHead(400).end();
      }
    });
  }).listen(8666);
}
declare const window: any;

export async function jsLogger(): Promise<string> {
  const iife = (
    await transform(
      function _(u: string) {
        const o = {
          log: console.log,
          info: console.info,
          warn: console.warn,
          error: console.error,
          trace: console.trace,
        };
        Object.keys(o).forEach(s => ((console as any)[s] = (...a: any[]) => r(s as keyof typeof o, ...a)));
        async function r(s: keyof typeof o, ...a: any[]) {
          o[s](...a);
          if (
            await fetch(u, {
              method: 'POST',
              body: JSON.stringify({ [s]: a }),
            })
              .then(e => e.ok)
              .catch(() => false)
          )
            return;
          Object.assign(console, o);
        }
        r('log', (window as any).navigator.userAgent);
      }.toString(),
      { loader: 'ts', minify: true, target: 'es2018' },
    )
  ).code;
  return `(${iife})('${typeof env.remoteLog === 'string' ? env.remoteLog : 'http://localhost:8666'}');`;
}
