import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';

window.lishogi.ready.then(() => {
  $('#form3-teams').each(function () {
    const textarea = this as HTMLTextAreaElement;

    new Textcomplete(new TextareaEditor(textarea), [
      {
        id: 'team',
        match: /(^|\s)(.+)$/,
        index: 2,
        search(term: string, callback: (res: any[]) => void) {
          window.lishogi.xhr
            .json('GET', '/team/autocomplete', { url: { term } }, { cache: 'default' })
            .then(
              (res: any[]) => {
                const current = textarea.value
                  .split('\n')
                  .map(t => t.split(' ')[0])
                  .slice(0, -1);
                callback(res.filter(t => !current.includes(t.id)));
              },
              _ => callback([]),
            );
        },
        template: team => team.name + ', by ' + team.owner + ', with ' + team.members + ' members',
        replace: team => '$1' + team.id + ' "' + team.name + '" by ' + team.owner + '\n',
      },
    ]);
  });
});
