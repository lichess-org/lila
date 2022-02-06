import * as xhr from 'common/xhr';

interface Team {
  id: string;
  name: string;
  owner: string;
  members: number;
}

lichess.load.then(() => {
  $('#form3-teams').each(function (this: HTMLTextAreaElement) {
    const textarea = this;

    const textcomplete = new window.Textcomplete(new window.Textcomplete.editors.Textarea(textarea), {
      dropdown: {
        maxCount: 10,
        placement: 'bottom',
      },
    });

    textcomplete.register([
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
            _ => callback([])
          );
        },
        template: (team: Team) => team.name + ', by ' + team.owner + ', with ' + team.members + ' members',
        replace: (team: Team) => '$1' + team.id + ' "' + team.name + '" by ' + team.owner + '\n',
      },
    ]);

    textcomplete.on('rendered', function () {
      if (textcomplete.dropdown.items.length) {
        // Activate the first item by default.
        textcomplete.dropdown.items[0].activate();
      }
    });
  });
});
