#!/usr/bin/env node

import { Client, type estypes } from '@elastic/elasticsearch';
import { MongoClient, type MongoClientOptions } from 'mongodb';
import { createServer, type Server } from 'node:http';
import process from 'node:process';

import type { Args, MongoDoc, Operations, IndexSchema, IndexName, Context } from './types.ts';

const usage = `usage:
  ./bin/elastic/ingest.ts [forum|ublog|team|study|game]* [options]

overview:
  if --from or --to is given:
    backfill the elasticsearch indices using a date-bounded mongodb search
  otherwise:
    watch mongo's elasticsearch_events collection as a durable mailbox and update es indices until SIGINT.
    track health metrics and expose for prometheus scraping on --metrics-port

arguments:
  forum ublog team study game  # the index names to operate on; default all

options:
  --help                     # show this help message and exit
  --es-uri=<uri>             # default 'http://127.0.0.1:9200'
  --mongo-uri=<uri>          # default 'mongodb://127.0.0.1:27017/lichess'
  --from=<YYYY[-MM[-DD]]|0>  # backfill only: inclusive UTC start date; --from or --from=0 means epoch
                             # YYYY or YYYY-MM also accepted and treated as first month/day of the month/year
  --to=<YYYY[-MM[-DD]]>      # backfill only: inclusive UTC end date; omit for no upper bound
                             # YYYY or YYYY-MM also accepted and treated as first month/day of the month/year
  --metrics-port=<port>      # watch only - prometheus /metrics port; default 9464, 0 to disable
  --interval=<seconds>       # watch only - the polling interval that governs how frequently the
                             # elasticsearch_events mailbox is drained and metrics are updated; default 10s

examples:
  # watch mode (all indices, 127.0.0.1, 30s interval etc):
  ./bin/elastic/ingest.ts

  # index every document and exit:
  ./bin/elastic/ingest.ts --from=0

  # backfill forum and team indices for the first half of 2025 through july 25 and exit:
  ./bin/elastic/ingest.ts forum team --from=2025 --to=2025-7-25

  # watch mode with all defaults given explicitly
  ./bin/elastic/ingest.ts forum ublog team study game \\
                          --es-uri=http://127.0.0.1:9200 \\
                          --mongo-uri=mongodb://127.0.0.1:27017/lichess \\
                          --metrics-port=9464 \\
                          --interval=30\n`;

let mongoClient: MongoClient;
let metricsServer: Server | undefined;

const indexNames: IndexName[] = ['forum', 'ublog', 'team', 'study', 'game']; // each with .ts file
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

  mongoClient = new MongoClient(args.mongoUri, { appName: 'es-index' } as MongoClientOptions);

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
}

async function watch(context: Context) {
  const mailbox = context.mongo.collection<WatchEvent>('elasticsearch_events');
  console.log(`watching ${args.mongoUri}/elasticsearch_events for ${args.indexes.join(', ')}...`);

  while (true) {
    let upserted = 0;
    let deleted = 0;
    try {
      const esEvents = await mailbox
        .find({ index: { $in: args.indexes } })
        .sort({ createdAt: 1, _id: 1 })
        .toArray();

      const deduped = [...new Map(esEvents.map(e => [`${e.index}:${e.docId}`, e])).values()];

      for (const [index, events] of Map.groupBy(deduped, e => e.index)) {
        const deletedIds = events.filter(e => e.operation === 'delete').map(e => e.docId);
        const toUpsert = events.filter(e => e.operation === 'upsert').map(e => e.docId);
        const mapping = indexing[index].mapping;
        const mongoDocs = await context.mongo
          .collection<MongoDoc>(mapping.collection)
          .find({ _id: { $in: toUpsert } }, { projection: mapping.projection })
          .toArray();
        const operations = await mapping.operations(mongoDocs, context);
        const liveIds = new Set([...operations.toUpsert.map(doc => doc.id), ...operations.toDelete]);
        const toDelete = [...operations.toDelete, ...toUpsert.filter(id => !liveIds.has(id)), ...deletedIds];

        await writeToIndex(index, { ...operations, toDelete });
        upserted += operations.toUpsert.length;
        deleted += toDelete.length;
      }
      await mailbox.deleteMany({ _id: { $in: esEvents.map(event => event._id) } });
      metrics.consecutiveFailures = 0;
    } catch (e) {
      metrics.cycles.error++;
      metrics.consecutiveFailures++;
      console.log(e instanceof Error ? (e.stack ?? e.message) : String(e));
    } finally {
      metrics.cycle = { timestamp: Date.now() / 1000, upserted, deleted, ok: !!metrics.consecutiveFailures };
      await new Promise(resolve => setTimeout(resolve, context.args.interval * 1000));
    }
  }
}

// ===========================================================================================================

async function backfill(index: IndexName, context: Context) {
  const mapping = indexing[index].mapping;
  console.log(`${index}: backfill started`);

  const cursor = context.mongo
    .collection<MongoDoc>(mapping.collection)
    .find(mapping.mongoFilter(args), { projection: mapping.projection });
  if (mapping.mongoSort) cursor.sort(mapping.mongoSort);

  let docs: MongoDoc[] = [];
  for await (const doc of cursor) {
    docs.push(doc);
    if (docs.length >= indexing[index].batchSize) {
      await writeToIndex(index, await mapping.operations(docs, context));
      const { upserted, deleted } = metrics.indexing[index];
      console.log(`${index}: ${upserted} indexed, ${deleted} deleted`);
      docs = [];
    }
  }
  if (docs.length) await writeToIndex(index, await mapping.operations(docs, context));
  const { upserted, deleted } = metrics.indexing[index];
  console.log(`${index}: backfill complete, ${upserted} indexed, ${deleted} deleted`);
}

// ===========================================================================================================

async function writeToIndex(index: IndexName, { toUpsert, toDelete }: Operations) {
  if (toUpsert.length + toDelete.length === 0) return;
  const esIndex = indexing[index].esPath;
  const body: estypes.BulkOperationContainer[] = [];
  for (const upsertMe of toUpsert) {
    body.push({ index: { _index: esIndex, _id: upsertMe.id } }, upsertMe.doc);
  }
  for (const deleteMe of toDelete) {
    body.push({ delete: { _index: esIndex, _id: deleteMe } });
  }
  const result = await esClient.bulk({ body });
  if (result.body.errors)
    throw new Error(`bulk '${index}' update failed: ${JSON.stringify(result.body).slice(0, 2000)}...`);
  metrics.indexing[index].upserted += toUpsert.length;
  metrics.indexing[index].deleted += toDelete.length;
}

// ===========================================================================================================

async function ensureIndex(config: IndexSchema) {
  if ((await esClient.indices.exists({ index: config.esPath })).body) return;
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

function parseArgs(): Args {
  const stringArgs = ['es-host', 'mongo-uri', 'metrics-port', 'from', 'to'];
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
    else if (indexNames.includes(arg as IndexName)) parsed.indexes.push(arg as IndexName);
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
      `es_index_cycle_ok ${metrics.cycle.ok}`,
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
