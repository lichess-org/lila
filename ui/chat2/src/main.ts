import { sayHello } from "./greet";

module.exports = function(node: Node, opts: Object) {

  console.log(node, opts, sayHello("TypeScript"));
};
