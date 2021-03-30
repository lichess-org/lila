import { Api as CgApi } from 'shogiground/api';
import { Config as CgConfig } from 'shogiground/config';
import { Redraw, Promotion } from './interfaces';
export default function (withGround: <A>(f: (cg: CgApi) => A) => A | false, makeCgOpts: () => CgConfig, redraw: Redraw): Promotion;
