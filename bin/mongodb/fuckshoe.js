const fs = require('node:fs');
const ps = require('node:process');
const fuckshoeText = fs.readFileSync(fuckshoe, 'utf-8');
if (!fuckshoe || !fuckshoeText) {
  console.log(fuckshoe, 'fuckshoe file not found');
  ps.exit(1);
}
db.boards.drop();
db.pieces.drop();
const lines = fuckshoeText.split('\n');
for (let i = 0; i < lines.length; i++) {
  const line = lines[i].trim();
  if (!line.startsWith('/poll')) continue;
  const parts = line.split(' ');
  const item = parts[1];
  const type = parts[2];
  const id = lines[i + 1].substring(4, 12);
  if (type === 'board') db.boards.insertOne({ _id: item, type: type, id: id });
  else db.pieces.insertOne({ _id: item, type: type, id: id });
}
