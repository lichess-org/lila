#!/usr/bin/env python3

# Analyzes module dependencies in build.sbt
# Outputs all and essential dependencies for each module to stdout
# Generates a png graph in the repo root folder

import os
import re
import networkx as nx
import pydot

buildfile_path = os.path.join(os.path.dirname(__file__), os.pardir,
                              'build.sbt')
output_path = os.path.join(os.path.dirname(__file__), os.pardir,
                           'dependency-graph.png')

def_module_pattern = r'^lazy val (\w+) = module\(.*'

G = nx.DiGraph()

with open(buildfile_path, 'r') as f:
    lines = iter(f)
    for line in lines:
        match = re.match(def_module_pattern, line)
        if match:
            module = match.group(1)
            if module != 'api':
                next_line = next(lines).strip()
                dependencies = [
                    dep for dep in next_line[4:-2].split(', ') if dep != ""
                ]
                for dependency in dependencies:
                    G.add_edge(dependency, module)

topological_generations = list(nx.topological_generations(G))
transitive_closure = nx.transitive_closure(G)


def build_providers_dict(dependencies):

    def add(res, subdependency, provider):
        if subdependency in res:
            res[subdependency].append(provider)
        else:
            res[subdependency] = [provider]

    providers_dict = {}
    for dependency, subdependencies in dependencies:
        add(providers_dict, dependency, dependency)
        for dep in subdependencies:
            add(providers_dict, dep, dependency)
    return providers_dict


def pick_essential_dependencies(providers_dict):

    def pick(essentials, providers_dict):
        if len(providers_dict) == 0:
            return essentials, providers_dict

        candidate = min(providers_dict.values(), key=len)
        if len(candidate) > 1:
            return essentials, providers_dict

        new = candidate[0]
        essentials.append(new)

        leftovers = {
            dependency: providers
            for dependency, providers in providers_dict.items()
            if new not in providers
        }

        return pick(essentials, leftovers)

    return pick([], providers_dict)


essentials_dict = {}
for node in G.nodes:
    all_dependencies = list(transitive_closure.predecessors(node))
    providers_dict = build_providers_dict([
        (dep, transitive_closure.predecessors(dep)) for dep in all_dependencies
    ])
    essentials, leftover = pick_essential_dependencies(providers_dict)
    essentials_dict[node] = essentials

    print("Module:", node)
    print("All dependencies:", all_dependencies)
    print("Essential dependencies:", essentials)
    if len(leftover) > 0:
        print("Leftover dependencies:", [dep for dep in leftover])
    print()

pydot_graph = nx.drawing.nx_pydot.to_pydot(G)
# pydot_graph.set_rankdir('LR')
pydot_graph.set_ratio(0.4)

sink_nodes = pydot.Subgraph(rank="same")

for generation in topological_generations:
    layer = pydot.Subgraph(rank="same")
    for node_name in generation:
        node = pydot_graph.get_node(node_name)[0]
        if G.out_degree(node_name) == 0:
            sink_nodes.add_node(node)
        else:
            layer.add_node(node)
    pydot_graph.add_subgraph(layer)

pydot_graph.add_subgraph(sink_nodes)

original_edges = set(G.edges())
for module in essentials_dict:
    for dependency in essentials_dict[module]:
        if (dependency, module) in original_edges:
            edge = pydot_graph.get_edge(dependency, module)[0]
        else:
            edge = pydot.Edge(dependency, module)
            pydot_graph.add_edge(edge)

        edge.set_color('#88aaff')
        edge.set_penwidth(1)
        edge.set_arrowsize(0)

pydot_graph.write_png(output_path)
