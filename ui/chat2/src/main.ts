// import { sayHello } from "./greet";

// module.exports = function(node: Node, opts: Object) {

//   console.log(node, opts, sayHello("TypeScript"));
// }

import { init } from 'snabbdom';
var patch = init([ // Init patch function with chosen modules
  require('snabbdom/modules/class').default, // makes it easy to toggle classes
  require('snabbdom/modules/props').default, // for setting properties on DOM elements
  require('snabbdom/modules/style').default, // handles styling on elements with support for animations
  require('snabbdom/modules/eventlisteners').default, // attaches event listeners
]);
var h = require('snabbdom/h').default; // helper function for creating vnodes

var vnode;

var data = {
  selected: undefined,
  movies: [
    {rank: 1, title: 'This is an', desc: 'Lorem ipsum dolor sit amet, sed pede integer vitae bibendum, accumsan sit, vulputate aenean tempora ipsum. Lorem sed id et metus, eros posuere suspendisse nec nunc justo, fusce augue placerat nibh purus suspendisse. Aliquam aliquam, ut eget. Mollis a eget sed nibh tincidunt nec, mi integer, proin magna lacus iaculis tortor. Aliquam vel arcu arcu, vivamus a urna fames felis vel wisi, cursus tortor nec erat dignissim cras sem, mauris ac venenatis tellus elit.'},
    {rank: 2, title: 'example of', desc: 'Consequuntur ipsum nulla, consequat curabitur in magnis risus. Taciti mattis bibendum tellus nibh, at dui neque eget, odio pede ut, sapien pede, ipsum ut. Sagittis dui, sodales sem, praesent ipsum conubia eget lorem lobortis wisi.'},
    {rank: 3, title: 'Snabbdom', desc: 'Quam lorem aliquam fusce wisi, urna purus ipsum pharetra sed, at cras sodales enim vestibulum odio cras, luctus integer phasellus.'},
    {rank: 4, title: 'doing hero transitions', desc: 'Et orci hac ultrices id in. Diam ultrices luctus egestas, sem aliquam auctor molestie odio laoreet. Pede nam cubilia, diam vestibulum ornare natoque, aenean etiam fusce id, eget dictum blandit et mauris mauris. Metus amet ad, elit porttitor a aliquet commodo lacus, integer neque imperdiet augue laoreet, nonummy turpis lacus sed pulvinar condimentum platea. Wisi eleifend quis, tristique dictum, ac dictumst. Sem nec tristique vel vehicula fringilla, nibh eu et posuere mi rhoncus.'},
    {rank: 5, title: 'using the', desc: 'Pede nam cubilia, diam vestibulum ornare natoque, aenean etiam fusce id, eget dictum blandit et mauris mauris. Metus amet ad, elit porttitor a aliquet commodo lacus, integer neque imperdiet augue laoreet, nonummy turpis lacus sed pulvinar condimentum platea. Wisi eleifend quis, tristique dictum, ac dictumst. Sem nec tristique vel vehicula fringilla, nibh eu et posuere mi rhoncus.'},
    {rank: 6, title: 'module for hero transitions', desc: 'Sapien laoreet, ligula elit tortor nulla pellentesque, maecenas enim turpis, quae duis venenatis vivamus ultricies, nunc imperdiet sollicitudin ipsum malesuada. Ut sem. Wisi fusce nullam nibh enim. Nisl hymenaeos id sed sed in. Proin leo et, pulvinar nunc pede laoreet.'},
    {rank: 7, title: 'click on ar element in', desc: 'Accumsan quia, id nascetur dui et congue erat, id excepteur, primis ratione nec. At nulla et. Suspendisse lobortis, lobortis in tortor fringilla, duis adipiscing vestibulum voluptates sociosqu auctor.'},
    {rank: 8, title: 'the list', desc: 'Ante tellus egestas vel hymenaeos, ut viverra nibh ut, ipsum nibh donec donec dolor. Eros ridiculus vel egestas convallis ipsum, commodo ut venenatis nullam porta iaculis, suspendisse ante proin leo, felis risus etiam.'},
    {rank: 9, title: 'to witness', desc: 'Metus amet ad, elit porttitor a aliquet commodo lacus, integer neque imperdiet augue laoreet, nonummy turpis lacus sed pulvinar condimentum platea. Wisi eleifend quis, tristique dictum, ac dictumst.'},
    {rank: 10, title: 'the effect', desc: 'Et orci hac ultrices id in. Diam ultrices luctus egestas, sem aliquam auctor molestie odio laoreet. Pede nam cubilia, diam vestibulum ornare natoque, aenean etiam fusce id, eget dictum blandit et mauris mauris'},
  ]
};

function select(m) {
  data.selected = m;
  render();
}

function render() {
  vnode = patch(vnode, view(data));
}

const fadeInOutStyle = {
  opacity: '0', delayed: {opacity: '1'}, remove: {opacity: '0'}
};

const detailView = (movie) =>
h('div.page', {style: fadeInOutStyle}, [
  h('div.header', [
    h('div.header-content.detail', {
      style: {opacity: '1', remove: {opacity: '0'}},
    }, [
      h('div.rank', [
        h('span.header-rank.hero', {hero: {id: 'rank'+movie.rank}}, movie.rank),
        h('div.rank-circle', {
          style: {transform: 'scale(0)',
            delayed: {transform: 'scale(1)'},
            destroy: {transform: 'scale(0)'}},
        }),
      ]),
      h('div.hero.header-title', {hero: {id: movie.title}}, movie.title),
      h('div.spacer'),
      h('div.close', {
        on: {click: [select, undefined]},
        style: {transform: 'scale(0)',
          delayed: {transform: 'scale(1)'},
          destroy: {transform: 'scale(0)'}},
      }, 'x'),
    ]),
  ]),
  h('div.page-content', [
    h('div.desc', {
      style: {opacity: '0', transform: 'translateX(3em)',
        delayed: {opacity: '1', transform: 'translate(0)'},
        remove: {opacity: '0', position: 'absolute', top: '0', left: '0',
          transform: 'translateX(3em)'}
      }
    }, [
      h('h2', 'Description:'),
      h('span', movie.desc),
    ]),
  ]),
]);

const overviewView = (movies) =>
h('div.page', {style: fadeInOutStyle}, [
  h('div.header', [
    h('div.header-content.overview', {
      style: fadeInOutStyle,
    }, [
      h('div.header-title', {
        style: {transform: 'translateY(-2em)',
          delayed: {transform: 'translate(0)'},
          destroy: {transform: 'translateY(-2em)'}}
      }, 'Top 10 movies'),
      h('div.spacer'),
    ]),
  ]),
  h('div.page-content', [
    h('div.list', {
      style: {opacity: '0', delayed: {opacity: '1'},
        remove: {opacity: '0', position: 'absolute', top: '0', left: '0'}}
    }, movies.map((movie) =>
                  h('div.row', {
                    on: {click: [select, movie]},
                  }, [
                    h('div.hero.rank', [
                      h('span.hero', {hero: {id: 'rank'+movie.rank}}, movie.rank)
                    ]),
                    h('div.hero', {hero: {id: movie.title}}, movie.title)
                  ])
                 )),
  ]),
]);

const view = (data) =>
h('div.page-container', [
  data.selected ? detailView(data.selected) : overviewView(data.movies),
]);

module.exports = (node: Node, opts: Object) => {
  vnode = patch(node, view(data));
  render();
}
