#!/usr/bin/env node

import { MongoClient } from 'mongodb';
import { setTimeout as sleep } from 'node:timers/promises';
import fs from 'node:fs';
import crypto from 'node:crypto';

// consider a wrapper script to provide common options (like the together.ai api key).
// bin/ublog-automod (with no extension) is .gitignored for this purpose.
// example bin/ublog-automod:
//   #!/bin/bash
//   "$(dirname "$0")/ublog-automod.mjs" --key=your-together-api-key --host=your-mongo-host "$@"

// see also:
//   file://./../../sysadmin/prompts/ublog-system-prompt.txt
//   file://./../modules/ublog/src/main/UblogAutomod.scala

const usage = `
usage: ./ublog-automod.mjs --help
       ./ublog-automod.mjs --key=your_together_ai_api_key [options] [arguments]
       ./ublog-automod.mjs --merge=out_file.ndjson [options]

overview:
  assess ublog posts with a together.ai llm using db.flag.ublogAutomodPrompt and process results.
  store results in ndjson files with --out option or directly update db.ublog_post. ndjsons can be
  merged later into db.ublog_post using the --merge option

  --key is required for assessment i.e. --key=tgp_v1_12345678-ABCDEFGHIJKLMNOPQRSTUV
    in assessment mode:      
      - if no --out option is given, results are incrementally stored in db.ublog_post
      - provide post ids, --[to/from] filters, or process all of db.ublog_post
      - stored hashes will prevent reprocessing posts in both --out and direct db mode.
        progress is saved and resumeable on abort. to force reprocessing, use --force

required (one of these):
  --dry-run                    # count matching posts with stale hashes
  --key=<together-ai-api-key>  # together.ai api key (required for any automod assessment)
  --merge=<ndjson file>        # merge existing automod object file into db.ublog_post

optional (with --key, --merge, or --dry-run):
  --host=<mongodb host>  # (default '127.0.0.1:27017')
  --db=<mongodb db>      # (default 'lichess')

optional (with --key or --dry-run):
  --from=<YYYY-MM-DD>    # process posts created on or after this date (inclusive UTC from 00:00)
  --to=<YYYY-MM-DD>      # process posts created on or before this date (inclusive UTC until 23:59)
  --print-ids            # print ids that require processing or were successfully processed
  id1 id2 ...            # process only id1, id2, etc (overrides --from/to)

optional (with --key only):
  --out=<ndjson file>    # output file for { _id, automod } objects, otherwise assessments go in ublog_post
  --log=<log file>       # append errors to log file (default silent)
  --ppm=<int>            # throttle posts per minute (default 500 - https://docs.together.ai/docs/rate-limits)
  --force                # retrieve new automod data even if hashes and versions match

examples:
  ./ublog-automod.mjs --dry-run --from=2024-01-01 --print-ids
    count posts from 2024 onwards that lack or have outdated automod results, print the ids and exit

  ./ublog-automod.mjs --key=xyzabc1234 --from=2025-01-01 --out=objects.ndjson
    assess posts from 2025 onwards, incrementally save results to 'objects.ndjson',
    reuse results from previous 'objects.ndjson' if it exists where hashes match

  ./ublog-automod.mjs --key=xyzabc1234 --force ublogId1 ublogId2
    fetches automod assessments for ublogId1 & ublogId2 and force update ublog_post
    regardless of hashes\n`;

const qualities = { spam: 0, weak: 1, good: 2, great: 3 }; // in sync with UblogAutomod.scala
const schemaVersion = 1;
const flushEvery = 100; // bulk write after every <flushEvery> assessments
const concurrentRequests = 32;
const model = 'Qwen/Qwen3-235B-A22B-Thinking-2507';

let client = undefined;

const args = parseArgs();
const retry = {};
const bulkOps = [];

const url = `mongodb://${args.host}/${args.db}?directConnection=true&serverSelectionTimeoutMS=2000&appName=ublog-automod`;
client = new MongoClient(url);
const db = client.db();

if (args.merge) await mergeAndExit();

const prompt = (await db.collection('flag').findOne({ _id: 'ublogAutomodPrompt' })).setting;
const temperature = 0; //(await db.collection('flag').findOne({ _id: 'ublogAutomodTemperature' }))?.setting ?? 0.3;
const posts = await db
  .collection('ublog_post')
  .find(getQuery(), { projection: { _id: 1, title: 1, intro: 1, markdown: 1, automod: 1 } })
  .toArray();
const progress = { total: posts.length, processed: 0, errors: 0 };
const previous = previousAutomods();
let nextAllowedAfter = Date.now();

await Promise.all(Array.from({ length: concurrentRequests }, worker));
await client.close();

report();

// ===========================================================================================================

async function worker() {
  const post = posts.shift();
  if (!post) return undefined;
  const id = post._id;
  retry[id] ??= { count: 0, result: 'unchanged' };
  try {
    const automod = await assess(post);
    if (automod) {
      if (args.out) {
        await fs.promises.appendFile(args.out, JSON.stringify({ _id: post._id, automod }) + '\n');
      } else if (needsUpdate(post.automod, automod.hash)) {
        bulkOps.push({ updateOne: { filter: { _id: post._id }, update: { $set: { automod } } } });
      }
    }
    progress.processed++;
  } catch (e) {
    if (args.log) await fs.promises.appendFile(args.log, `${id} ${e}\n`);

    progress.errors++;
    if (++retry[id].count < 5) {
      posts.push(post); // try again later
    } else if (args.log) {
      await fs.promises.appendFile(args.log, `${id} gave up on post: ${JSON.stringify(post)}\n);`);
    }
  }
  await maybeBulkWrite(Math.min(flushEvery, posts.length)); // crash on exception
  return worker();
}

// ===========================================================================================================

async function assess(post) {
  const content = `${post.title} ${post.intro} ${post.markdown}`.slice(0, 40_000); // UblogAutomod.scala
  const hash = crypto.createHash('sha256').update(content).digest('hex').slice(0, 12);
  const id = post._id;
  const existing = previous[id] ?? post.automod;
  const backoff = retry[id].result !== 'network' ? 0 : 5_000 * 2 ** (retry[id].count - 1);
  const sleepTime = Math.max(0, nextAllowedAfter - Date.now()) + backoff;

  if (!needsUpdate(existing, hash)) return existing;
  if (args.dryRun) {
    retry[id].result = 'dirty';
    return false;
  }

  showProgress();
  nextAllowedAfter += (60 * 1000) / args.ppm;
  await sleep(sleepTime);

  const response = await fetch('https://api.together.xyz/v1/chat/completions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${args.key}` },
    body: JSON.stringify({
      model,
      max_tokens: 4096,
      temperature: temperature + 0.1 * retry[id].count,
      messages: [
        { role: 'system', content: prompt },
        { role: 'user', content },
      ],
    }),
  });
  if (response.status === 429) {
    const retryAfter = Number(response.headers.get('Retry-After'));
    nextAllowedAfter = Date.now() + (isNaN(retryAfter) ? 30 : retryAfter) * 1000;
  }
  if (!response.ok) {
    retry[id].result = 'network';
    throw response.statusText;
  }

  const body = await response.text();
  if (args.log) await fs.promises.appendFile(args.log, `${id} full response: ${body}\n`);
  try {
    const data = JSON.parse(body).choices[0].message.content;
    const automod = normalize(JSON.parse(/\{[^}]+\}/.exec(data)[0]));
    automod.hash = hash;
    retry[id].result = 'success';
    return automod;
  } catch {
    retry[id].result = 'bad payload';
    throw `bad response: ${body.slice(0, 100_000)}`;
  }
}

// ===========================================================================================================

function previousAutomods() {
  const prev = Object.fromEntries(posts.filter(p => p.automod).map(p => [p._id, p.automod]));

  if (args.out && fs.existsSync(args.out)) {
    for (const { _id, automod } of fs
      .readFileSync(args.out, 'utf-8')
      .split('\n')
      .filter(Boolean)
      .map(JSON.parse)) {
      prev[_id] = automod;
    }
    fs.truncateSync(args.out, 0);
  }
  return prev;
}

// ===========================================================================================================

function needsUpdate(automod, hash) {
  if (!automod || args.force) return true;
  if (automod.version !== schemaVersion) return true;
  if (automod.hash !== hash) return true;
  return false;
}

// ===========================================================================================================

function normalize(original) {
  const q = original.quality?.trim().toLowerCase();
  if (!(q in qualities)) throw 'bad quality: ' + JSON.stringify(original);
  const fixed = { quality: qualities[q], version: schemaVersion };
  if (q === 'good' || q === 'great')
    fixed.evergreen =
      typeof original.evergreen === 'boolean'
        ? original.evergreen
        : original.evergreen?.toLowerCase() === 'true';
  if (q !== 'spam') maybeCopy(original, fixed, 'commercial');
  maybeCopy(original, fixed, 'flagged');
  return fixed;
}

// ===========================================================================================================

function maybeCopy(src, dest, key) {
  if (typeof src[key] !== 'string') return;
  const v = src[key].trim().toLowerCase();
  if (v === '' || v === 'none' || v === 'false') return;
  dest[key] = src[key].trim();
}

// ===========================================================================================================

async function maybeBulkWrite(bufferSize) {
  if (bulkOps.length === 0 || bulkOps.length < bufferSize) return;
  const ops = bulkOps.slice();
  bulkOps.length = 0;
  return await db.collection('ublog_post').bulkWrite(ops, { ordered: false });
}

// ===========================================================================================================

async function mergeAndExit() {
  if (!fs.existsSync(args.merge)) await exit(`merge file '${args.merge}' not found`, 1);
  let [msg, code] = [undefined, 1];
  try {
    for (const { _id, automod } of fs
      .readFileSync(args.merge, 'utf-8')
      .split('\n')
      .filter(Boolean)
      .map(JSON.parse)) {
      bulkOps.push({
        updateOne: { filter: { _id }, update: { $set: { automod } } },
      });
      await maybeBulkWrite(1000);
    }
    await maybeBulkWrite(bulkOps.length);
    code = 0;
  } catch (e) {
    msg = `merge error: ${String(e)} ${JSON.stringify(e)}`;
  }
  await exit(msg, code);
}

// ===========================================================================================================

function report() {
  process.stdout.clearLine(0);
  process.stdout.cursorTo(0);

  const [successIds, errorIds, dirtyIds] = [[], [], []];

  for (const [id, r] of Object.entries(retry)) {
    if (r.result === 'dirty') dirtyIds.push(id);
    else if (r.result === 'success' || r.result === 'unchanged') successIds.push(id);
    else errorIds.push(id);
  }

  if (args.dryRun) {
    console.log(`${dirtyIds.length}/${progress.total} needs update`);
    if (args.printIds) console.log(dirtyIds.join(' '));
  } else {
    if (args.printIds) console.log(`succeeded: ${successIds.join(' ')}`);
    if (errorIds.length > 0) {
      const report = `failed: ` + errorIds.join(' ');
      console.error(report);
      if (args.log) fs.appendFileSync(args.log, report + '\n');
    }
  }
}

// ===========================================================================================================

function parseArgs() {
  const boolArgs = ['dry-run', 'print-ids', 'force'];
  const stringArgs = ['host', 'db', 'from', 'to', 'key', 'out', 'merge', 'log'];
  const numberArgs = ['ppm'];

  const args = { ppm: 500, host: '127.0.0.1', db: 'lichess', ids: [] };
  process.argv.slice(2).forEach(arg => {
    if (arg === '--help' || arg === '-h') exit(usage, 0);
    else if (/^[A-Za-z0-9]{8}$/.test(arg)) args.ids.push(arg);
    else if (!parseDashArg(arg)) exit(`bad argument: ${arg}`, 1);
  });
  if (!args.key && !args.merge && !args.dryRun)
    exit('missing --dry-run, --merge, or --key=together-ai-api-key', 1);
  if (isNaN(args.ppm) || args.ppm <= 0 || args.ppm > 10000) exit(`bad --ppm=${args.ppm}`, 1);
  return args;

  function parseDashArg(arg) {
    if (!arg.startsWith('--')) return false;
    const [key, value] = arg.slice(2).split('=');
    const jsKey = key.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
    if (boolArgs.includes(key)) args[jsKey] = [undefined, 'true', '1'].includes(value);
    else if (stringArgs.includes(key)) args[jsKey] = value;
    else if (numberArgs.includes(key)) args[jsKey] = Number(value);
    else return false;
    return true;
  }
}

// ===========================================================================================================

function getQuery() {
  const filters = [{ live: { $eq: true } }];
  if (args.ids.length > 0) filters.push({ _id: { $in: args.ids } });
  else {
    if (args.from) {
      const fromDate = new Date(`${args.from}T00:00:00.000Z`);
      if (isNaN(fromDate.valueOf())) exit(`bad --from date ${args.from}. use YYYY-MM-DD`, 1);
      filters.push({ 'lived.at': { $gte: fromDate } });
    }
    if (args.to) {
      const toDate = new Date(`${args.to}T23:59:59.999Z`);
      if (isNaN(toDate.valueOf())) exit(`bad --to date ${args.to}. use YYYY-MM-DD`, 1);
      filters.push({ 'lived.at': { $lte: toDate } });
    }
  }
  return { $and: filters };
}

// ===========================================================================================================

function showProgress() {
  const { processed, total, errors } = progress;
  process.stdout.clearLine(0);
  process.stdout.cursorTo(0);
  process.stdout.write(
    `${processed}/${total} processed, ${errors} retries, ${Math.floor((processed * 100) / total)}% complete`,
  );
}

// ===========================================================================================================

function exit(message = undefined, code = 0) {
  if (message) console.log(message);
  if (!client) process.exit(code);
  return client.close().then(() => process.exit(code));
}
