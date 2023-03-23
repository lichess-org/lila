import * as ps from 'node:process';
import * as fs from 'node:fs';
import { Readable } from 'node:stream';
import { finished } from 'node:stream/promises';

// requires node 18.x

const defaultCrowdvFile = 'crowdv-27-02-2023.json';

let builder: Builder;

const buildMode: SubRestriction = { del: true, sub: 2 }; // allow dels and/or specify max sub length

function buildCostMap(
  subMap: Map<string, SubInfo>, // the map of all valid substitutions within --max-ops distance
  freqThreshold: number, // minimum frequency of a substitution to be considered
  countThreshold: number // minimum count for a substitution to be considered
) {
  const costMax = 0.9;
  const subCostMin = 0.4;
  const delCostMin = 0.2;

  // we don't do anything with crowdv json confs, don't trust em
  const costs = [...subMap.entries()]
    .filter(([_, e]) => e.freq >= freqThreshold && e.count >= countThreshold)
    .sort((a, b) => b[1].freq - a[1].freq);

  costs.forEach(([_, v], i) => {
    v.cost = ((costMax - subCostMin) * i) / costs.length + (v.tpe === 'del' ? delCostMin : subCostMin);
  });
  return new Map(costs);
}

async function main() {
  const subMap = new Map<string, SubInfo>();
  const opThreshold = parseInt(getArg('max-ops') ?? '1');
  const freqThreshold = parseFloat(getArg('freq') ?? '0.002');
  const countThreshold = parseInt(getArg('count') ?? '6');
  const outfile = getArg('out') ?? '../src/voiceMoveGrammar.ts';
  const lexfile = getArg('lex') ?? 'en-us.json';
  builder = new Builder(lexfile);
  const entries = (await parseCrowdvData(getArg('in') ?? defaultCrowdvFile))
    .map(data => makeLexEntry(data))
    .filter(x => x) as LexEntry[];

  for (const e of entries.filter(e => e.h != e.x)) {
    parseTransforms(findTransforms(e.h, e.x, buildMode), e, subMap, opThreshold);
  }
  subMap.forEach(v => (v.freq = v.count / v.all));

  buildCostMap(subMap, freqThreshold, countThreshold).forEach((sub, key) => {
    ppCost(key, sub);
    const [from, to] = key.split(' ');
    builder.addSub(from, { to: to, cost: sub.cost ?? 1 });
  });
  writeGrammar(outfile);
}

// flatten list of transforms into sub map
function parseTransforms(xss: Transform[][], entry: LexEntry, subMap: Map<string, SubInfo>, opThreshold = 1) {
  return xss
    .filter(xss => xss.length <= opThreshold)
    .forEach(xs =>
      xs.forEach(x => {
        const cost = subMap.get(`${x.from} ${x.to}`) ?? {
          tpe: x.to === '' ? 'del' : 'sub',
          count: 0,
          all: builder.occurrences.get(x.from || x.to)!,
          conf: 0,
          freq: 0,
        };
        cost.count++;
        cost.conf += x.at < entry.c.length ? entry.c[x.at] : 0;
        subMap.set(`${x.from} ${x.to}`, cost);
      })
    );
}

// find transforms to turn h (heard) into x (exact)
function findTransforms(
  h: string,
  x: string,
  mode: SubRestriction,
  pos = 0, // for recursion
  line: Transform[] = [],
  lines: Transform[][] = [],
  crumbs = new Map<string, number>()
): Transform[][] {
  if (h === x) return [line];
  if (pos >= x.length && !mode.del) return [];
  if (crumbs.has(h + pos) && crumbs.get(h + pos)! <= line.length) return [];
  crumbs.set(h + pos, line.length);

  return validOps(h, x, pos, mode).flatMap(({ hnext, op }) =>
    findTransforms(
      hnext,
      x,
      mode,
      pos + (op === 'skip' ? 1 : op.to.length),
      op === 'skip' ? line : [...line, op],
      lines,
      crumbs
    )
  );
}

function validOps(h: string, x: string, pos: number, mode: SubRestriction) {
  const validOps: { hnext: string; op: Transform | 'skip' }[] = [];
  if (h[pos] === x[pos]) validOps.push({ hnext: h, op: 'skip' });
  const minSlice = mode.del !== true || validOps.length > 0 ? 1 : 0;
  let slen = Math.min(mode.sub ?? 0, x.length - pos);
  while (slen >= minSlice) {
    const slice = x.slice(pos, pos + slen);
    if (pos < h.length && !(slen > 0 && h.startsWith(slice, pos)))
      validOps.push({
        hnext: h.slice(0, pos) + slice + h.slice(pos + 1),
        op: { from: h[pos], at: pos, to: slice }, // replace h[pos] with slice
      });
    slen--;
  }
  return validOps;
}

function makeLexEntry(entry: CrowdvData): LexEntry | undefined {
  const xset = new Set([...builder.encode(entry.exact)]);
  const hunique = [...new Set([...builder.encode(entry.heard)])];
  if (hunique.filter(h => xset.has(h)).length < xset.size - 2) return undefined;
  if (entry.heard.endsWith(' next')) entry.heard = entry.heard.slice(0, -5);
  builder.addOccurrence(entry.heard); // for token frequency
  return {
    h: builder.encode(entry.heard),
    x: builder.encode(entry.exact),
    c: entry.data.map(x => x.conf),
  };
}

function ppCost(key: string, e: SubInfo) {
  const grey = (s: string) => `\x1b[30m${s}\x1b[0m`;
  const red = (s: string) => `\x1b[31m${s}\x1b[0m`;
  const nameC = (s: string) => `\x1b[36m${s}\x1b[0m`;
  const opC = (s: string) => `\x1b[0m${s}\x1b[0m`;
  const valueC = (s: string) => `\x1b[0m${s}\x1b[0m`;
  const prettyPair = (k: string, v: string) => `${nameC(k)}${grey(':')} ${valueC(v)}`;
  const [from, to] = key.split(' ').map(x => builder.wordsOf(x));
  console.log(
    `'${opC(from)}${grey(' => ')}${to === '' ? red('<delete>') : opC(to)}'${grey(':')} { ` +
      [
        prettyPair('count', `${e.count}`),
        prettyPair('all', `${e.all}`),
        prettyPair('conf', (e.conf / e.count).toFixed(2)),
        prettyPair('freq', e.freq.toFixed(3)),
        prettyPair('cost', e.cost?.toFixed(2) ?? '1'),
      ].join(grey(', ')) +
      ` }${grey(',')}`
  );
}

function writeGrammar(out: string) {
  fs.writeFileSync(
    out,
    '// *************************** this file is generated. see ui/input/@build/README.md ***************************\n\n' +
      'export type Sub = { to: string, cost: number };\n\n' +
      'export type Entry = { in: string, tok: string, tags: string[], val?: string, subs?: Sub[] };\n\n' +
      `export const lexicon: Entry[] = ${JSON.stringify(builder.lexicon, null, 2)};`
  );
}

function getArg(arg: string): string | undefined {
  return ps.argv
    .filter(v => v.startsWith(`--${arg}`))
    .pop()
    ?.slice(3 + arg.length);
}

async function parseCrowdvData(file: string) {
  if (!fs.existsSync(file)) {
    if (parseInt(ps.versions.node.split('.')[0]) < 18) {
      console.log(`Node 18+ required, you're running ${ps.version}\n\n`);
      ps.exit(1);
    }
    let url = file;
    if (/https?:/.test(url)) file = file.split('/').pop() ?? 'crowdv.json';
    else url = `https://raw.githubusercontent.com/schlawg/crowdv/master/${file}`;

    try {
      const { ok, statusText, body } = await (globalThis as any).fetch(url);
      if (!ok) throw new Error(statusText);
      const stream = fs.createWriteStream(file);
      await finished(Readable.fromWeb(body).pipe(stream));
      stream.close();
    } catch (e) {
      console.log(`${e} - ${url}`);
      ps.exit(1);
    }
  }
  return JSON.parse(fs.readFileSync(file, 'utf-8')) as CrowdvData[];
}

type SubRestriction = { del?: boolean; sub?: number };

type LexEntry = {
  h: string;
  x: string;
  c: number[];
};

type Transform = {
  from: string; // single token, or empty string for insertion
  to: string; // one or more tokens, or empty string for erasure
  at: number; // index (for breadcrumbs)
};

type SubInfo = {
  tpe: 'del' | 'sub';
  all: number;
  count: number;
  freq: number;
  conf: number;
  cost?: number;
};

type CrowdvData = {
  heard: string;
  exact: string;
  round: number;
  ip: string;
  data: Array<{
    word: string;
    start: number;
    end: number;
    conf: number;
  }>;
};

type Sub = {
  to: string;
  cost: number;
};

type Entry = {
  in: string; // the word or phrase recognized by kaldi, unique in lexicon
  tok?: string; // single char token representation (or multiple for a phrase)
  val?: string; // the string moveHandler receives, default is tok
  subs?: Sub[]; // allowable token transitions calculated by this script
  tags?: string[]; // classificiation context for this token, used downstream
};

class Builder {
  occurrences = new Map<string, number>();
  tokenVal = new Map<string, string>();
  wordToken = new Map<string, string>();
  phrases: Entry[] = [];
  lexicon: Entry[];
  constructor(lexfile = 'en-us.json') {
    this.lexicon = JSON.parse(fs.readFileSync(`lexicon/${lexfile}`, 'utf-8')) as Entry[];
    const reserved = this.lexicon.map(t => t.tok ?? '').join('') + `,"'`;
    const available = Array.from({ length: 93 }, (_, i) => String.fromCharCode(33 + i))
      .filter(x => !reserved.includes(x))
      .concat(Array.from({ length: 128 }, (_, i) => String.fromCharCode(256 + i)));

    for (const e of this.lexicon) {
      if (e.in.includes(' ')) {
        this.phrases.push(e);
        continue;
      }
      if (e.tok === undefined) {
        if (reserved.includes(e.in)) e.tok = e.in;
        else e.tok = available.shift();
      } else if (e.tok === ' ' || e.tok === ',') throw new Error(`Illegal token for ${e.in}`);
      const tok = e.tok as string;
      this.wordToken.set(e.in, tok);
      this.tokenVal.set(tok, e.val ?? '');
      e.subs = [{ to: '', cost: e.tags?.includes('ignore') ? 0 : 0.5 }];
    }
    for (const e of this.phrases) {
      // need tokens for all words in phrases
      const words = e.in.split(' ');
      let phraseToks = '';
      for (const word of words) {
        const tok = this.wordToken.get(word) ?? available.shift();
        if (!this.wordToken.has(word)) {
          const part: Entry = { in: word, tok: tok!, tags: ['part'] };
          this.lexicon.push(part);
          this.wordToken.set(word, tok!);
          this.tokenVal.set(tok!, part.in);
          part.subs = [{ to: '', cost: 0.5 }];
        }
        phraseToks += tok;
      }
      e.tok = phraseToks;
    }
  }
  addOccurrence(phrase: string) {
    this.encode(phrase)
      .split('')
      .forEach(token => this.occurrences.set(token, (this.occurrences.get(token) ?? 0) + 1));
  }
  addSub(token: string, sub: Sub) {
    const tok = this.lexicon.find(e => e.tok === token)!;
    if (!tok.subs) tok.subs = [sub];
    else {
      const s = tok.subs.find(s => s.to === sub.to)!;
      if (s) s.cost = Math.min(s.cost, sub.cost);
      else tok.subs.push(sub);
    }
  }
  tokenOf(word: string) {
    return this.wordToken.get(word) ?? ('12345678'.includes(word) ? word : '');
  }
  fromToken(token: string) {
    return this.tokenVal.get(token) ?? token;
  }
  encode(phrase: string) {
    return this.wordToken.has(phrase)
      ? this.tokenOf(phrase)
      : phrase
          .split(' ')
          .map(word => this.tokenOf(word))
          .join('');
  }
  decode(tokens: string) {
    return tokens
      .split('')
      .map(token => this.fromToken(token))
      .join(' ');
  }
  wordsOf(tokens: string) {
    return tokens
      .split('')
      .map(token => [...this.wordToken.entries()].find(([_, tok]) => tok === token)?.[0])
      .join(' ');
  }
}

main();
