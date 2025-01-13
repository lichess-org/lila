import { copyLocalPackage, copyVendorPackage } from './util.js';

const vendorsJs: [string, string[]][] = [
  ['howler', ['dist/howler.core.min.js']],
  ['jquery', ['dist/jquery.min.js']],
  ['spectrum', ['dist/spectrum.min.js', 'dist/spectrum.min.css']],
  ['fairy-stockfish-nnue.wasm', ['stockfish.js', 'stockfish.wasm', 'stockfish.worker.js']],
  ['shogiground', ['dist/shogiground.min.js']],
  ['jquery-powertip', ['dist/jquery.powertip.min.js']],
  ['infinite-scroll', ['dist/infinite-scroll.pkgd.min.js']],
  ['sortablejs', ['Sortable.min.js']],
  [
    '@liskadan/sliding-puzzles',
    ['dist/iife/sliding-puzzles.min.js', 'assets/css/hakoirimusume.css'],
  ],
  ['@yaireo/tagify', ['dist/tagify.js']],
];

await Promise.all(
  vendorsJs.map(([packageName, fileNames]) => copyVendorPackage(packageName, fileNames)),
);

const localJs: string[] = ['fipr', 'yaneuraou.k-p', 'typeahead'];

await Promise.all(localJs.map(pkg => copyLocalPackage(pkg)));
