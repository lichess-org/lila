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
       ./ublog-automod.mjs --merge=your_object_file.ndjson [options]

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
  --key=<together-ai-api-key>  # together.ai api key (required for any automod assessment)
  --merge=<ndjson file>        # merge existing automod object file into db.ublog_post

optional (with --key or --merge):
  --host=<mongodb host>  # (default '127.0.0.1:27017')
  --db=<mongodb db>      # (default 'lichess')

optional (with --key only):
  --out=<ndjson file>    # output file for { _id, automod } objects, otherwise assessments go in ublog_post
  --log=<log file>       # log file for errors (default silent)
  --ppm=<int>            # throttle posts per minute (default 500 - https://docs.together.ai/docs/rate-limits)
  --from=<YYYY-MM-DD>    # process posts created on or after this date (inclusive UTC from 00:00)
  --to=<YYYY-MM-DD>      # process posts created on or before this date (inclusive UTC until 23:59)
  --force                # retrieve new automod data even if hashes match (for schema changes)
  id1 id2 ...            # process only id1, id2, etc (overrides --from/to)

examples:
  ./ublog-automod.mjs --key=xyzabc1234 --from=2025-01-01 --out=objects.ndjson
    assess posts from 2025 onwards, incrementally save results to 'objects.ndjson',
    reuse results from previous 'objects.ndjson' if it exists where hashes match

  ./ublog-automod.mjs --key=xyzabc1234 --force ublogId1 ublogId2
    fetches automod assessments for ublogId1 & ublogId2 and force update ublog_post
    regardless of hashes\n`;

const concurrentRequests = 32;
const model = 'Qwen/Qwen3-235B-A22B-fp8-tput';
let [client, processed, errors] = [undefined, 0, 0];

const args = parseArgs();

const url = `mongodb://${args.host}/${args.db}?directConnection=true&serverSelectionTimeoutMS=2000&appName=ublog-automod`;
console.log(url);
client = new MongoClient(url);
const db = client.db();

if (args.merge) await mergeAndExit();

const prompt = (await db.collection('flag').findOne({ _id: 'ublogAutomodPrompt' })).setting;
const posts = await db
  .collection('ublog_post')
  .find(getQuery(), { projection: { _id: 1, title: 1, intro: 1, markdown: 1, automod: 1 } })
  .toArray();
const total = posts.length;
const previous = new Map(posts.filter(p => p.automod).map(p => [p._id, p.automod]));

if (args.out && fs.existsSync(args.out)) {
  for (const { _id, automod } of fs
    .readFileSync(args.out, 'utf-8')
    .split('\n')
    .filter(Boolean)
    .map(JSON.parse)) {
    previous.set(_id, automod);
  }
  fs.truncateSync(args.out, 0);
}
let nextAllowed = Date.now();

await Promise.all(Array.from({ length: concurrentRequests }, worker));
await client.close();

process.stdout.clearLine(0);
process.stdout.cursorTo(0);

// ===========================================================================================================

async function worker() {
  const post = posts.shift();
  if (!post) return;

  try {
    const automod = await assess(post);
    if (args.out) {
      await fs.promises.appendFile(args.out, JSON.stringify({ _id: post._id, automod }) + '\n');
    } else if (automod.hash !== post.automod?.hash || args.force) {
      await db.collection('ublog_post').updateOne({ _id: post._id }, { $set: { automod } });
    }
    processed++;
  } catch (e) {
    if (args.log) await fs.promises.appendFile(args.log, `${post._id} ${e}\n`);
    errors++;
    posts.push(post);
  }
  return worker();
}

// ===========================================================================================================

async function assess(post) {
  const content = `${post.title} ${post.intro} ${post.markdown}`.slice(0, 40_000); // UblogAutomod.scala
  const hash = crypto.createHash('sha256').update(content).digest('hex').slice(0, 12);
  if (!args.force) {
    if (hash === post.automod?.hash) return post.automod;
    else if (hash === previous.get(post._id)?.hash) return previous.get(post._id);
  }

  showProgress();

  const sleepTime = Math.max(0, nextAllowed - Date.now());
  nextAllowed += (60 * 1000) / args.ppm;

  await sleep(sleepTime);

  const response = await fetch('https://api.together.xyz/v1/chat/completions', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${args.key}` },
    body: JSON.stringify({
      model,
      messages: [
        { role: 'system', content: prompt },
        { role: 'user', content },
      ],
    }),
  });

  if (!response.ok) throw response.statusText;

  const body = await response.text();
  try {
    const data = JSON.parse(body).choices[0].message.content;
    const automod = normalize(JSON.parse(/\{[^}]+\}/.exec(data)[0]));
    automod.hash = hash;
    return automod;
  } catch {
    throw `bad response: ${body.slice(0, 100_000)}`;
  }
}

// ===========================================================================================================

function normalize(original) {
  const copy = {};
  for (const [key, v] of Object.entries(original)) {
    if (typeof v === 'string' && ['', 'none', 'reason'].includes(v.trim().toLowerCase())) continue;
    else if (typeof v !== 'string' && key !== 'evergreen') continue;
    copy[key] = v;
  }
  if (!['good', 'weak', 'spam'].includes(copy.classification))
    throw 'bad classification: ' + JSON.stringify(original);
  if (copy.classification !== 'good') delete copy.evergreen;
  if (copy.classification === 'spam') {
    delete copy.offtopic;
    delete copy.commercial;
  }
  return copy;
}

// ===========================================================================================================

async function mergeAndExit() {
  if (!fs.existsSync(args.merge)) await exit(`merge file '${args.merge}' not found`, 1);
  let [msg, code] = [undefined, 1];
  const ops = [];
  try {
    for (const { _id, automod } of fs
      .readFileSync(args.merge, 'utf-8')
      .split('\n')
      .filter(Boolean)
      .map(JSON.parse)) {
      ops.push({ updateOne: { filter: { _id }, update: { $set: { automod } } } });
      if (ops.length >= 1000) {
        await db.collection('ublog_post').bulkWrite(ops, { ordered: false });
        ops.length = 0;
      }
    }
    if (ops.length > 0) await db.collection('ublog_post').bulkWrite(ops, { ordered: false });
    code = 0;
  } catch (e) {
    msg = `merge error: ${JSON.stringify(e)}`;
  }
  await exit(msg, code);
}

// ===========================================================================================================

function parseArgs() {
  const args = { ppm: 500, host: '127.0.0.1', db: 'lichess', ids: [] };
  process.argv.slice(2).forEach(arg => {
    if (arg === '--help' || arg === '-h') exit(usage, 0);
    else if (arg.startsWith('--host=')) args.host = arg.slice(7);
    else if (arg.startsWith('--db=')) args.db = arg.slice(5);
    else if (arg === '--force') args.force = true;
    else if (arg.startsWith('--from=')) args.from = arg.slice(7);
    else if (arg.startsWith('--to=')) args.to = arg.slice(5);
    else if (arg.startsWith('--key=')) args.key = arg.slice(6);
    else if (arg.startsWith('--out=')) args.out = arg.slice(6);
    else if (arg.startsWith('--merge=')) args.merge = arg.slice(8);
    else if (arg.startsWith('--log=')) args.log = arg.slice(6);
    else if (arg.startsWith('--ppm=')) {
      args.ppm = parseInt(arg.slice(6), 10);
      if (isNaN(args.ppm) || args.ppm <= 0 || args.ppm > 10000) exit(`bad --ppm: ${arg.slice(6)}`);
    } else if (/^[A-Za-z0-9]{8}$/.test(arg)) args.ids.push(arg);
    else exit(`bad argument: ${arg}`, 1);
  });
  if (!args.key && !args.merge) exit('missing --key=together-ai-api-key', 1);
  return args;
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
  process.stdout.clearLine(0);
  process.stdout.cursorTo(0);
  process.stdout.write(
    `${processed}/${total} processed, ${errors} retries, ${Math.round((processed * 100) / total)}% complete`,
  );
}

// ===========================================================================================================

function exit(message = undefined, code = 0) {
  if (message) console.log(message);
  if (!client) process.exit(code);
  return client.close().then(() => process.exit(code));
}
