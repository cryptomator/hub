/**
 * Describes a segment (slice) for a pie chart.
 * @param index The segment index (0-based)
 * @param total Total number of segments
 * @param radius Radius of the circle
 * @returns SVG path string
 */
export function describeSegment(index: number, total: number, radius: number): string {
  const angle = (2 * Math.PI) / total;
  const startAngle = index * angle - Math.PI / 2;
  const endAngle = startAngle + angle;
  const x1 = 18 + radius * Math.cos(startAngle);
  const y1 = 18 + radius * Math.sin(startAngle);
  const x2 = 18 + radius * Math.cos(endAngle);
  const y2 = 18 + radius * Math.sin(endAngle);
  const largeArc = angle > Math.PI ? 1 : 0;
  return `M18,18 L${x1},${y1} A${radius},${radius} 0 ${largeArc} 1 ${x2},${y2} Z`;
}
