import { State } from './state';
import * as cg from './types';
export declare type Mutation<A> = (state: State) => A;
export declare type AnimVector = cg.NumberQuadShift;
export interface AnimVectors {
    [key: string]: AnimVector;
}
export interface AnimCaptures {
    [key: string]: cg.Piece;
}
export interface AnimFadings {
    [key: string]: cg.Piece;
}
export interface AnimRoles {
    [key: string]: cg.Role;
}
export interface AnimPlan {
    anims: AnimVectors;
    fadings: AnimFadings;
    captures: AnimCaptures;
    tempRole: AnimRoles;
    nextPlan?: AnimPlan;
}
export interface AnimCurrent {
    start: cg.Timestamp;
    frequency: cg.KHz;
    plan: AnimPlan;
    lastMove?: cg.Key[];
}
export declare function anim<A>(mutation: Mutation<A>, state: State, fadeOnly?: boolean, noCaptSequences?: boolean): A;
export declare function render<A>(mutation: Mutation<A>, state: State): A;
export declare function isObjectEmpty(o: any): boolean;
