#!/usr/bin/env node

import { Client, type estypes } from '@elastic/elasticsearch';
import { MongoClient, type MongoClientOptions } from 'mongodb';
import { createServer, type Server } from 'node:http';
import process from 'node:process';
import { createInterface } from 'node:readline/promises';
import { isDeepStrictEqual } from 'node:util';

import type { Args, MongoDoc, Operations, IndexSchema, IndexName, Context } from './types.ts';

const usage = `usage:
  ./bin/elastic/ingest.ts [forum|ublog|team|study|game|feed]* [options]

overview:
  if --from or --to is given:
    backfill the elasticsearch indices using a date-bounded mongodb search
  otherwise:
    watch mongo's elasticsearch_events collection as a durable mailbox and update es indices until SIGINT.
    process bounded batches and flag rejected events so later watch cycles skip them.
    track health metrics and expose for prometheus scraping on --metrics-port

arguments:
  forum ublog team study game feed  # the index names to operate on; default all

options:
  --help                     # show this help message and exit
  --es-uri=<uri>             # default 'http://127.0.0.1:9200'
  --mongo-uri=<uri>          # default 'mongodb://127.0.0.1:27017/lichess'
  --from=<YYYY[-MM[-DD]]|0>  # backfill only: inclusive UTC start date; --from=0 means epoch
                             # YYYY or YYYY-MM also accepted and treated as first month/day of the month/year
  --to=<YYYY[-MM[-DD]]>      # backfill only: inclusive UTC end date; omit for no upper bound
                             # YYYY or YYYY-MM also accepted and treated as first month/day of the month/year
  --metrics-port=<port>      # watch only - prometheus /metrics port; default 9464, 0 to disable
  --interval=<seconds>       # watch only - the polling interval that governs how frequently the
                             # elasticsearch_events mailbox is drained and metrics are updated; default 10s

examples:
  # watch mode (all indices, 127.0.0.1, 10s interval etc):
  ./bin/elastic/ingest.ts

  # index every document and exit:
  ./bin/elastic/ingest.ts --from=0

  # backfill forum and team indices for the first half of 2025 through july 25 and exit:
  ./bin/elastic/ingest.ts forum team --from=2025 --to=2025-7-25

  # watch mode with all defaults given explicitly
  ./bin/elastic/ingest.ts forum ublog team study game feed \\
                          --es-uri=http://127.0.0.1:9200 \\
                          --mongo-uri=mongodb://127.0.0.1:27017/lichess \\
                          --metrics-port=9464 \\
                          --interval=10\n`;

let mongoClient: MongoClient;
let metricsServer: Server | undefined;

const indexNames: IndexName[] = ['forum', 'ublog', 'team', 'study', 'game', 'feed'];
const args = parseArgs();
const esClient = new Client({ node: args.esUri });
const indexing = Object.fromEntries(
  await Promise.all(
    args.indexes.map(async name => [name, await import(`./${name}.ts`).then(m => m.getIndexSchema())]),
  ),
) as Record<IndexName, IndexSchema>;
const metrics = {
  cycle: { timestamp: Date.now() / 1000, upserted: 0, deleted: 0, ok: true },
  cycles: { ok: 0, error: 0 },
  consecutiveFailures: 0,
  indexing: Object.fromEntries(indexNames.map(index => [index, { upserted: 0, deleted: 0 }])) as Record<
    IndexName,
    { upserted: number; deleted: number }
  >,
};

try {
  if (args.mode === 'watch' && args.metricsPort > 0) await startMetrics();

  mongoClient = new MongoClient(args.mongoUri, { appName: 'es-index' });

  console.log(`connecting to ${args.mongoUri}...`);
  await mongoClient.connect();

  const context = { args, mongo: mongoClient.db() };

  for (const index of args.indexes) {
    await ensureIndex(indexing[index]);
    if (args.mode === 'backfill') await backfill(index, context);
  }
  if (args.mode === 'watch') await watch(context);
} catch (e) {
  await exit(e instanceof Error ? (e.stack ?? e.message) : String(e), 1);
}
await exit(undefined, 0);

// ===========================================================================================================

interface WatchEvent {
  index: IndexName;
  docId: string;
  operation: 'upsert' | 'delete';
  createdAt?: Date;
  rejected?: boolean;
}

async function watch(context: Context) {
  console.log(`watching ${args.mongoUri}/elasticsearch_events for ${args.indexes.join(', ')}...`);

  while (true) {
    let upserted = 0;
    let deleted = 0;
    let pending = false;
    let ok = false;
    try {
      const result = await indexEvents(context);
      for (const [index, written] of result.results) {
        metrics.indexing[index].upserted += written.upserted;
        metrics.indexing[index].deleted += written.deleted;
        upserted += written.upserted;
        deleted += written.deleted;
      }
      ok = result.ok;
      pending = ok && result.pending;
    } catch (e) {
      console.error(e);
    } finally {
      metrics.cycles[ok ? 'ok' : 'error']++;
      metrics.consecutiveFailures = ok ? 0 : metrics.consecutiveFailures + 1;
      metrics.cycle = {
        timestamp: Date.now() / 1000,
        upserted,
        deleted,
        ok,
      };
      if (!pending) await new Promise(resolve => setTimeout(resolve, context.args.interval * 1000));
    }
  }
}

// ===========================================================================================================

async function indexEvents(context: Context) {
  const mailbox = context.mongo.collection<WatchEvent>('elasticsearch_events');
  const results = new Map<IndexName, WriteResult>();
  let ok = true;
  let pending = false;

  for (const index of context.args.indexes) {
    try {
      const schema = indexing[index];
      const events = await mailbox
        .find({ index, rejected: { $ne: true } })
        .sort({ createdAt: 1, _id: 1 })
        .limit(schema.batchSize)
        .toArray();
      if (!events.length) continue;
      const deduped = [...new Map(events.map(event => [event.docId, event])).values()];
      const upsertIds = deduped.filter(event => event.operation === 'upsert').map(event => event.docId);
      const docs = await context.mongo
        .collection<MongoDoc>(schema.mapping.collection)
        .find({ _id: { $in: upsertIds } }, { projection: schema.mapping.projection })
        .toArray();
      const operations = await schema.mapping.operations(docs, context);
      const upserted = new Set(operations.toUpsert.map(doc => doc.id));
      operations.toDelete = [
        ...new Set([
          ...operations.toDelete,
          ...upsertIds.filter(id => !upserted.has(id)),
          ...deduped.filter(event => event.operation === 'delete').map(event => event.docId),
        ]),
      ];

      const result = await writeToIndex(index, operations);
      results.set(index, result);
      ok &&= result.ok;
      const completed = events.filter(event => result.completed.has(event.docId));
      if (completed.length)
        await mailbox.deleteMany({
          index,
          $or: [
            { _id: { $in: completed.map(event => event._id) } },
            ...deduped
              .filter(event => result.completed.has(event.docId))
              .map(event => ({
                docId: event.docId,
                rejected: true,
                _id: { $lt: event._id },
              })),
          ],
        });
      const rejected = events.filter(event => result.rejected.has(event.docId));
      if (rejected.length)
        await mailbox.updateMany(
          { _id: { $in: rejected.map(event => event._id) } },
          { $set: { rejected: true } },
        );
      pending ||= events.length === schema.batchSize && completed.length + rejected.length > 0;
    } catch (error) {
      console.error(`${index}:`, error);
      ok = false;
    }
  }
  return { results, ok, pending };
}

// ===========================================================================================================

async function backfill(index: IndexName, context: Context) {
  const mapping = indexing[index].mapping;
  console.log(`${index}: backfill started`);

  const write = async (docs: MongoDoc[]) => {
    const result = await writeToIndex(index, await mapping.operations(docs, context));
    metrics.indexing[index].upserted += result.upserted;
    metrics.indexing[index].deleted += result.deleted;
  };

  const cursor = context.mongo
    .collection<MongoDoc>(mapping.collection)
    .find(mapping.mongoFilter(args), { projection: mapping.projection });
  if (mapping.mongoSort) cursor.sort(mapping.mongoSort);

  let docs: MongoDoc[] = [];
  for await (const doc of cursor) {
    docs.push(doc);
    if (docs.length >= indexing[index].batchSize) {
      await write(docs);
      const { upserted, deleted } = metrics.indexing[index];
      console.log(`${index}: ${upserted} indexed, ${deleted} deleted`);
      docs = [];
    }
  }
  if (docs.length) await write(docs);
  const { upserted, deleted } = metrics.indexing[index];
  console.log(`${index}: backfill complete, ${upserted} indexed, ${deleted} deleted`);
}

// ===========================================================================================================

interface WriteResult {
  completed: Set<string>;
  rejected: Set<string>;
  upserted: number;
  deleted: number;
  ok: boolean;
}

async function writeToIndex(index: IndexName, operations: Operations) {
  const schema = indexing[index];
  const result: WriteResult = {
    completed: new Set(),
    rejected: new Set(),
    upserted: 0,
    deleted: 0,
    ok: true,
  };
  const maxBulkBytes = 5 * 1024 * 1024;
  let batch: { id: string; operation: 'index' | 'delete'; body: string }[] = [];
  let bytes = 0;

  const send = async (): Promise<boolean> => {
    if (!batch.length) return true;
    try {
      const { body } = await esClient.bulk<estypes.BulkResponse, string>({
        body: batch.map(entry => entry.body).join(''),
      });
      if (body.items.length !== batch.length)
        throw new Error(`bad bulk response: wanted ${batch.length} ops but got ${body.items.length}`);
      body.items.forEach((item, index) => {
        const entry = batch[index];
        const response = item[entry.operation]!;
        if (
          !response.error &&
          (response.status < 300 || (entry.operation === 'delete' && response.status === 404))
        ) {
          result.completed.add(entry.id);
          if (entry.operation === 'index') result.upserted++;
          else result.deleted++;
        } else {
          console.error(`${index}/${entry.id}: ${JSON.stringify(response)}`);
          if (response.status === 400 || response.status === 413) result.rejected.add(entry.id);
          else result.ok = false;
        }
      });
      batch = [];
      bytes = 0;
      return true;
    } catch (error) {
      console.error(`${index}:`, error);
      result.ok = false;
      return false;
    }
  };

  const entries = function* () {
    for (const entry of operations.toUpsert) {
      yield {
        id: entry.id,
        operation: 'index' as const,
        body: `${JSON.stringify({ index: { _index: schema.esPath, _id: entry.id } })}\n${JSON.stringify(entry.doc)}\n`,
      };
    }
    for (const id of operations.toDelete) {
      yield {
        id,
        operation: 'delete' as const,
        body: `${JSON.stringify({ delete: { _index: schema.esPath, _id: id } })}\n`,
      };
    }
  };

  for (const entry of entries()) {
    const size = Buffer.byteLength(entry.body);
    if (size > maxBulkBytes) {
      console.error(`${index}/${entry.id}: ${size} exceeds ${maxBulkBytes} bulk bytes`);
      result.rejected.add(entry.id);
      continue;
    }
    if (batch.length && (bytes + size > maxBulkBytes || batch.length >= schema.batchSize)) {
      if (!(await send())) return result;
    }
    batch.push(entry);
    bytes += size;
  }
  await send();
  return result;
}

// ===========================================================================================================

async function ensureIndex(config: IndexSchema) {
  if ((await esClient.indices.exists({ index: config.esPath })).body) {
    const existingBody = (await esClient.indices.get({ index: config.esPath })).body[config.esPath];
    const existingSettings = new Map(Object.entries(existingBody.settings?.index ?? {}));
    const { index: existingIndex, ...expectedSettings } = config.settings;
    const checks = [
      {
        field: '_source.enabled',
        existing: existingBody.mappings?._source?.enabled ?? true,
        expected: config.source,
      },
      { field: 'properties', existing: existingBody.mappings?.properties, expected: config.properties },
      ...Object.entries({ ...expectedSettings, ...existingIndex }).map(([field, expected]) => ({
        field,
        existing: existingSettings.get(field),
        expected,
      })),
    ];
    const changed = checks
      .filter(({ existing, expected }) =>
        expected !== null && typeof expected === 'object'
          ? !isDeepStrictEqual(existing, expected)
          : String(existing) !== String(expected),
      )
      .map(
        ({ field, existing, expected }) =>
          `  EXISTING: ${field}: ${JSON.stringify(existing)}\n  EXPECTED: ${JSON.stringify(expected)}\n`,
      );
    if (!changed.length) return;
    if (args.mode === 'watch' || !(await confirmRecreateIndex(config.esPath, changed)))
      throw `/${config.esPath} has changed. run a backfill to replace it`;
    await esClient.indices.delete({ index: config.esPath });
  }
  await esClient.indices.create({
    index: config.esPath,
    body: {
      settings: config.settings,
      mappings: { _source: { enabled: config.source }, properties: config.properties },
    },
  });
  console.log(`created /${config.esPath}`);
}

// ===========================================================================================================

async function confirmRecreateIndex(index: string, changed: string[]): Promise<boolean> {
  console.log(`/${index} index has changed.\n`);
  for (const difference of changed) console.log(difference);
  const readline = createInterface({ input: process.stdin, output: process.stdout });
  try {
    return /^[yY]/.test(await readline.question(`\nRecreate /${index} and backfill? (y/N) `));
  } finally {
    readline.close();
  }
}

// ===========================================================================================================

function parseArgs(): Args {
  const stringArgs = ['es-uri', 'mongo-uri', 'metrics-port', 'interval', 'from', 'to'];
  const parsed = {
    indexes: [],
    esUri: 'http://127.0.0.1:9200',
    mongoUri: 'mongodb://127.0.0.1:27017/lichess',
    metricsPort: '9464',
    from: undefined as string | undefined,
    to: undefined as string | undefined,
    interval: '10',
  } as Record<string, any>; // these are runtime validated, so 'any' just avoids ts complexity

  for (let i = 2; i < process.argv.length; i++) {
    const arg = process.argv[i];
    if (arg === '--help' || arg === '-h') exit(usage, 0);
    else if (arg.startsWith('--')) {
      const [key, maybeVal] = arg.slice(2).split('=');
      if (!stringArgs.includes(key)) exit(`bad argument: ${arg}`, 1);

      const jsKey = key.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
      if (maybeVal === undefined) {
        if (i + 1 >= process.argv.length || process.argv[i + 1].startsWith('-'))
          exit(`missing value for argument: ${arg}`, 1);
        parsed[jsKey] = process.argv[++i];
      } else {
        parsed[jsKey] = maybeVal;
      }
    } else if (arg === 'all') parsed.indexes = [...indexNames];
    else if (indexNames.includes(arg as IndexName)) parsed.indexes.push(arg);
    else exit(`bad argument: ${arg}`, 1);
  }
  if (parsed.indexes.length === 0) parsed.indexes = [...indexNames];

  if (!/^[a-z][a-z0-9+.-]*:\/\//i.test(parsed.mongoUri)) parsed.mongoUri = `mongodb://${parsed.mongoUri}`;
  const mongoUrl = new URL(parsed.mongoUri);
  if (mongoUrl.protocol !== 'mongodb:' && mongoUrl.protocol !== 'mongodb+srv:')
    exit('--mongo-uri must start with mongodb:// or mongodb+srv://', 1);
  if (mongoUrl.pathname === '/') parsed.mongoUri = mongoUrl.href + 'lichess';

  const interval = Number(parsed.interval);
  if (!Number.isFinite(interval) || interval <= 0) exit('--interval must be a positive number', 1);

  const metricsPort = Number.parseInt(parsed.metricsPort, 10);
  if (!Number.isInteger(metricsPort) || metricsPort < 0 || metricsPort > 65_535)
    exit('--metrics-port must be an integer from 0 to 65535', 1);

  return {
    mode: parsed.from !== undefined || parsed.to !== undefined ? 'backfill' : 'watch',
    indexes: [...new Set(parsed.indexes)] as IndexName[],
    esUri: new URL(parsed.esUri).toString().replace(/\/$/, ''),
    mongoUri: parsed.mongoUri,
    from: parsed.from && parsed.from !== '0' ? parseDate(parsed.from) : new Date(0),
    to: parsed.to ? parseDate(parsed.to) : undefined,
    metricsPort,
    interval,
  };
}

// ===========================================================================================================

async function startMetrics() {
  metricsServer = createServer((request, response) => {
    if (request.url !== '/metrics') {
      response.writeHead(404).end();
      return;
    }
    const lines = [
      '# HELP es_index_cycle_seconds_since_epoch Unix timestamp of the most recent watch cycle.',
      '# TYPE es_index_cycle_seconds_since_epoch gauge',
      `es_index_cycle_seconds_since_epoch ${metrics.cycle.timestamp}`,
      '# HELP es_index_cycle_upserts Documents upserted during the most recent watch cycle.',
      '# TYPE es_index_cycle_upserts gauge',
      `es_index_cycle_upserts ${metrics.cycle.upserted}`,
      '# HELP es_index_cycle_deletes Documents deleted during the most recent watch cycle.',
      '# TYPE es_index_cycle_deletes gauge',
      `es_index_cycle_deletes ${metrics.cycle.deleted}`,
      '# HELP es_index_cycle_events Mailbox events read during the most recent watch cycle.',
      '# TYPE es_index_cycle_events gauge',
      `es_index_cycle_events ${metrics.cycle.upserted + metrics.cycle.deleted}`,
      '# HELP es_index_cycle_ok Whether the most recent watch cycle completed successfully.',
      '# TYPE es_index_cycle_ok gauge',
      `es_index_cycle_ok ${Number(metrics.cycle.ok)}`,
      '# HELP es_index_cycles_total Watch cycles since process start.',
      '# TYPE es_index_cycles_total counter',
      `es_index_cycles_total ${metrics.cycles.ok + metrics.cycles.error}`,
      '# HELP es_index_successes_total Successful watch cycles since process start.',
      '# TYPE es_index_successes_total counter',
      `es_index_successes_total ${metrics.cycles.ok}`,
      '# HELP es_index_failures_total Failed watch cycles since process start.',
      '# TYPE es_index_failures_total counter',
      `es_index_failures_total ${metrics.cycles.error}`,
      '# HELP es_index_consecutive_failures Consecutive failed watch cycles.',
      '# TYPE es_index_consecutive_failures gauge',
      `es_index_consecutive_failures ${metrics.consecutiveFailures}`,
      '# HELP es_index_upserts_total Documents upserted since process start.',
      '# TYPE es_index_upserts_total counter',
      ...args.indexes.map(
        index => `es_index_upserts_total{index="${index}"} ${metrics.indexing[index].upserted}`,
      ),
      '# HELP es_index_deletes_total Documents deleted since process start.',
      '# TYPE es_index_deletes_total counter',
      ...args.indexes.map(
        index => `es_index_deletes_total{index="${index}"} ${metrics.indexing[index].deleted}`,
      ),
      '',
    ];
    response
      .writeHead(200, { 'Content-Type': 'text/plain; version=0.0.4; charset=utf-8' })
      .end(lines.join('\n'));
  });
  await new Promise<void>((resolve, reject) => {
    metricsServer?.once('error', reject);
    metricsServer?.listen(args.metricsPort, resolve);
  });
  console.log(`prometheus metrics on []:${args.metricsPort}/metrics`);
}

// ===========================================================================================================

function parseDate(dateStr: string): Date {
  const [year, month, day] = dateStr.split('-').map(Number);
  const date = new Date(Date.UTC(year, (month ?? 1) - 1, day ?? 1));
  if (isNaN(date.valueOf())) throw new Error(`bad date ${dateStr}. use YYYY-MM-DD`);
  return date;
}

// ===========================================================================================================

async function exit(message: string | undefined, code = 0): Promise<never> {
  if (message) console.log(message);
  metricsServer?.close();
  if (!mongoClient) process.exit(code); // if the mongo connection is not set up yet, callers need not await.
  await mongoClient.close();
  process.exit(code);
}
