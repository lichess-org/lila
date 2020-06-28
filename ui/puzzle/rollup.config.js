import resolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from '@rollup/plugin-typescript';
import { terser } from 'rollup-plugin-terser';

export default args => {
  return {
    input: 'src/main.ts',
    output: [
      args['config-prod'] ? {
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
      } : {
        file: '../../public/compiled/lichess.puzzle.js',
        format: 'iife',
        name: 'LichessPuzzle',
      }
    ],
    plugins: [
      resolve(),
      typescript(),
      commonjs({
        extensions: ['.js', '.ts'],
      }),
    ]
  };
}
