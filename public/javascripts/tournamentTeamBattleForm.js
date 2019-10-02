$(function() {

  $('#form3-teams').each(function() {

    const textarea = this;

    lichess.loadScript('vendor/textcomplete.js').then(function() {

      const textcomplete = new Textcomplete(new Textcomplete.editors.Textarea(textarea), {
        dropdown: {
          maxCount: 10,
          placement: 'bottom'
        }
      });

      textcomplete.register([{
        id: 'team',
        match: /(^|\s)(.+)$/,
        index: 2,
        search: function(term, callback) {
          $.ajax({
            url: "/team/autocomplete",
            data: {
              term: term
            },
            success: function(teams) {
              callback(teams);
            },
            error: function() {
              callback([]);
            },
            cache: true
          })
        },
        template: function(team, i) {
          return team.name + ', by ' + team.owner + ', with ' + team.members + ' members';
        },
        replace: function(team) {
          return '$1' + team.id + ' "' + team.name + '" by ' + team.owner + '\n'
        }
      }]);

      textcomplete.on('rendered', function() {
        if (textcomplete.dropdown.items.length) {
          // Activate the first item by default.
          textcomplete.dropdown.items[0].activate();
        }
      });
    });
  });
});
