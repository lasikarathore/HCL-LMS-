import { Color, ScaleType } from '@swimlane/ngx-charts';

/**
 * IMS v2 · PDF-aligned pastel analytics palette (fintech / soft donut + bar + line)
 */
export const IMS_CHART_SCHEME: Color = {
  name: 'imsPastel',
  selectable: true,
  group: ScaleType.Ordinal,
  domain: [
    '#8eb8d6',
    '#b8a9d4',
    '#e8a8b8',
    '#9ccbb0',
    '#e8c97a',
    '#a8c0e6',
    '#c4d4e8',
    '#d4b8c6',
  ],
};
