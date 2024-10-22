import * as xhr from 'common/xhr';
import { Textcomplete } from '@textcomplete/core';
import { TextareaEditor } from '@textcomplete/textarea';

interface Team {
  id: string;
  name: string;
  owner: string;
  members: number;
}

site.load.then(() => {
  $('#form3-teams').each(function (this: HTMLTextAreaElement) {
    const textarea = this;

    new Textcomplete(new TextareaEditor(textarea), [
      {
        id: 'team',
        match: /(^|\s)(.+)$/,
        index: 2,
        search(term: string, callback: (res: Team[]) => void) {
          xhr.json(xhr.url('/team/autocomplete', { term }), { cache: 'default' }).then(
            (res: Team[]) => {
              const current = textarea.value
                .split('\n')
                .map(t => t.split(' ')[0])
                .slice(0, -1);
              callback(res.filter(t => !current.includes(t.id)));
            },
            _ => callback([]),
          );
        },
        template: (team: Team) => team.name + ', by ' + team.owner + ', with ' + team.members + ' members',
        replace: (team: Team) => '$1' + team.id + ' "' + team.name + '" by ' + team.owner + '\n',
      },
    ]);
  });
});
