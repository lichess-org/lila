const http = require('http');
const fs = require('fs-extra');

const port = parseInt(process.argv[2]);
const dir = process.argv[3];
const delay = parseInt(process.argv[4] || 1000);

let files,
  file,
  completion = -1;

fs.readdir(dir).then(list => {
  files = list.filter(n => n.endsWith('.pgn'));
  serveIndex(0);
});

function serveIndex(index) {
  if (!files[index]) index = 0;
  const percent = Math.floor((index * 100) / files.length);
  if (percent > completion) {
    completion = percent;
    console.log(`${percent}%`);
  }
  file = files[index];
  setTimeout(() => serveIndex(index + 1, delay), delay);
}

http
  .createServer((request, response) => {
    const path = `${dir}/${file}`;
    return fs
      .readFile(path, {
        encoding: 'utf8',
      })
      .then(content => {
        console.log(`${path} ${content.length}`);
        response.end(content);
      });
  })
  .listen(port, err => {
    if (err) return console.log(err);
    console.log(`server is listening on ${port}`);
  });
