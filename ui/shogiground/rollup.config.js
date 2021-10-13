import typescript from '@rollup/plugin-typescript';
import { terser } from 'rollup-plugin-terser';

export default {
  input: 'src/index.ts',
  output: [
    {
      file: 'dist/shogiground.js',
      format: 'iife',
      name: 'Shogiground',
    },
    {
      file: 'dist/shogiground.min.js',
      format: 'iife',
      name: 'Shogiground',
      plugins: [terser()],
    },
  ],
  plugins: [typescript()],
};
