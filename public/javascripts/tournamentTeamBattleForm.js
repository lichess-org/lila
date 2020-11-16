$(function () {
  $("#form3-teams").each(function () {
    const textarea = this;

    function currentTeamIds() {
      return textarea.value
        .split("\n")
        .map((t) => t.split(" ")[0])
        .slice(0, -1);
    }

    lishogi.loadScript("vendor/textcomplete.min.js").then(function () {
      const textcomplete = new Textcomplete(
        new Textcomplete.editors.Textarea(textarea),
        {
          dropdown: {
            maxCount: 10,
            placement: "bottom",
          },
        }
      );

      textcomplete.register([
        {
          id: "team",
          match: /(^|\s)(.+)$/,
          index: 2,
          search: function (term, callback) {
            $.ajax({
              url: "/team/autocomplete",
              data: {
                term: term,
              },
              success: function (teams) {
                const current = currentTeamIds();
                callback(teams.filter((t) => !current.includes(t.id)));
              },
              error: function () {
                callback([]);
              },
              cache: true,
            });
          },
          template: function (team, i) {
            return (
              team.name +
              ", by " +
              team.owner +
              ", with " +
              team.members +
              " members"
            );
          },
          replace: function (team) {
            return (
              "$1" + team.id + ' "' + team.name + '" by ' + team.owner + "\n"
            );
          },
        },
      ]);

      textcomplete.on("rendered", function () {
        if (textcomplete.dropdown.items.length) {
          // Activate the first item by default.
          textcomplete.dropdown.items[0].activate();
        }
      });
    });
  });
});
