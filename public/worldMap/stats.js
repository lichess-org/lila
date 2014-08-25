$(function() {

    var topCountries = function() {
        var toSort = [];
        for (var c in stats.countries) {
            toSort.push([c, stats.countries[c]]);
        }
        toSort.sort(function(a, b) { return b[1] - a[1]; });
        var top10 = toSort.slice(0, 10);
        $('#topCountries').html('');
        $.each(top10, function(i, v) {
            $('#topCountries').append(
                '<div>'+v[0]+': '+v[1]+'</div>'
            );
        });
    };

    var updateStats = function() {
        $('#countries > span').text(Object.keys(stats.countries).length);
        topCountries();

        setTimeout(function() { updateStats(); }, 500);
    };

    updateStats();

});
