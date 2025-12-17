#!/usr/bin/env node

// https://github.com/lichess-org/lila/issues/18891

import { MongoClient } from 'mongodb';
import { createReadStream } from 'node:fs';
import { createInterface } from 'node:readline';

const file = '../tmp/20251203_filtered_puzzle_ids.txt';
const client = new MongoClient(
  'mongodb://127.0.0.1:27317/puzzler?directConnection=true&serverSelectionTimeoutMS=2000&appName=puzzle-flagger',
);
await client.connect();
const db = client.db();
const coll = db.collection('puzzle2_puzzle');

const rl = createInterface({ input: createReadStream(file), crlfDelay: Infinity });

const buffer = [];
let counter = 0;

async function flush() {
  await coll.updateMany({ _id: { $in: buffer }, tooSubtle: { $ne: true } }, { $set: { tooSubtle: true } });
  counter += buffer.length;
  buffer.length = 0;
  console.log(`Flagged ${counter} puzzles`);
}

try {
  for await (const line of rl) {
    buffer.push(line.trim());
    if (buffer.length >= 1000) await flush();
  }
} finally {
  await flush();
}
