import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessLearn',
    input: 'src/main.js',
    output: 'learn',
    js: true,
  },
});
