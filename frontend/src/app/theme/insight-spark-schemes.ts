import { Color, ScaleType } from '@swimlane/ngx-charts';

/** Mint, sky, sand sparklines — matches reference wallet cards */
export const INSIGHT_SPARK_SCHEMES: Color[] = [
  {
    name: 'sparkMint',
    selectable: false,
    group: ScaleType.Ordinal,
    domain: ['#4a9d7a'],
  },
  {
    name: 'sparkSky',
    selectable: false,
    group: ScaleType.Ordinal,
    domain: ['#4d7eaf'],
  },
  {
    name: 'sparkSand',
    selectable: false,
    group: ScaleType.Ordinal,
    domain: ['#c97d3a'],
  },
];
