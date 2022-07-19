import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessAnalyse',
    input: 'src/main.ts',
    output: 'analysisBoard', // can't call it analyse.js, triggers adblockers :facepalm:
  },
  study: {
    name: 'LichessAnalyse',
    input: 'src/plugins/studyMain.ts',
    output: 'analysisBoard.study',
  },
  nvui: {
    name: 'LichessAnalyseNvui',
    input: 'src/plugins/nvui.ts',
    output: 'analysisBoard.nvui',
  },
  studyTopicForm: {
    input: 'src/plugins/studyTopicForm.ts',
    output: 'study.topic.form',
  },
});
