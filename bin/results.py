#!/usr/bin/env python3

import json
import sys
import os

# type GameResult = {
#   result: 'white'|'black'|'draw'|'error';
#   reason: string;
#   white: string|{name: string};
#   black: string|{name: string};
#   movetime: number;
#   threads: number;
#   hash: number;
#   nfold?: number;
#   startingFen?: string;
#   moves: number | uci[];
# };

def main():
    width = 16 # crosstable width (minus 1 padding)
    engines = ['11hce','16hce','14nn12','16nn07','16nn12','16nn40','16nn60'] # preferred sort order
    template = {'W': 0, 'D': 0, 'L': 0}
    draws = {}
    xtable = {}
    results = []
    errors = []

    for x in range(1, len(sys.argv)):
        try:
            with open(sys.argv[x], 'r') as f:
                results.extend(json.load(f))
            print('Loaded ' + sys.argv[x])
        except IOError as e:
            print(str(e) + ' while loading ' + sys.argv[x])
    
    for game in results:
        outcome = game['result']
        reason = game['reason']
        w = short(game['white'])
        b = short(game['black'])
        if outcome == 'error' or outcome == 'draw' and reason.startswith('Stockfish'):
            errors.append(reason) # tolerate errors in some earlier batches
        elif outcome == 'draw':
            xtable.setdefault(w, {}).setdefault(b, template.copy())['D'] += 1
            xtable.setdefault(b, {}).setdefault(w, template.copy())['D'] += 1
            draws[reason] = draws.get(reason, 0) + 1
        else:
            winner = w if outcome == 'white' else b
            loser = b if outcome == 'white' else w
            xtable.setdefault(winner, {}).setdefault(loser, template.copy())['W'] += 1
            xtable.setdefault(loser, {}).setdefault(winner, template.copy())['L'] += 1

    print('Total games:', len(results) - len(errors))
    print('Draws by reason:')
    for reason, count in draws.items():
        print(f'  {reason}: {count}')
    print('Crosstable:\n| ' + 'row vs col'.ljust(width), end='| ')
    for e in engines:
        print(f"W/D/L vs {e}".ljust(width), end='| ')
    print()
    for _ in range(len(engines)+1):
        print('|'.ljust(width+2,'-'), end='')
    print('|')
    for e in engines:
        print('| ' + e.ljust(width), end='| ')
        for vsE in engines:
            try:
                print(wdl(xtable[e][vsE]).ljust(width), end='| ')
            except KeyError:
                print('       -'.ljust(width), end='| ')
        print()

def wdl(tally):
    return f"{str(tally['W']).ljust(4)}/ {str(tally['D']).ljust(4)}/ {str(tally['L']).ljust(4)}"

def short(engine): # don't want the full engine name
    try:
        engine = engine['name'][10:]
    except TypeError as e:
        engine = engine[10:] # compensate for a breaking change in the dataset format
    if engine == '11':
        return '11hce'
    elif engine == '16 HCE':
        return '16hce'
    elif engine == '14 NNUE':
        return '14nn12'
    elif engine.endswith('7MB'):
        return '16nn07'
    elif engine.endswith('12MB'):
        return '16nn12'
    elif engine.endswith('40MB'):
        return '16nn40'
    elif engine.endswith('60MB'):
        return '16nn60'
    return 'unknown'

if __name__ == '__main__':
    main()