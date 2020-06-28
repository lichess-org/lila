import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from '@rollup/plugin-typescript';
import { terser } from 'rollup-plugin-terser';

export default {
  input: 'src/main.ts',
  output: [
    {
      file: '../../public/compiled/lichess.puzzle.js',
      format: 'iife',
      name: 'LichessPuzzle',
    },
    {
      file: '../../public/compiled/lichess.puzzle.min.js',
      format: 'iife',
      name: 'LichessPuzzle',
      plugins: [
        terser({
          output: {
            safari10: true,
            comments: false,
          },
        }),
      ],
    },
  ],
  plugins: [
    resolve(),
    typescript(),
    commonjs({
      extensions: ['.js', '.ts'],
    }),
  ]
};
