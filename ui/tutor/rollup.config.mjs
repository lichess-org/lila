import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessTutor',
    input: 'src/main.ts',
    output: 'tutor',
  },
});
