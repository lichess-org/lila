import { multi } from '@build/rollupProject';

export default multi([
  {
    name: 'LichessAnalyse',
    input: 'src/main.ts',
    output: 'analysisBoard', // can't call it analyse.js, triggers adblockers :facepalm:
  },
  {
    input: 'src/plugins/nvui.ts',
    output: 'analysisBoard.nvui',
  },
  {
    input: 'src/plugins/studyTopicForm.ts',
    output: 'study.topic.form',
  },
]);
