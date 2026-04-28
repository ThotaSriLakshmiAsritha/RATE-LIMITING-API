export function LineChart({ points }) {
  if (!points.length) {
    return <p className="text-sm text-ink-500">No requests yet. Run a few API calls to render performance trends.</p>
  }

  const width = 640
  const height = 180
  const step = points.length > 1 ? width / (points.length - 1) : width

  const path = points
    .map((point, index) => {
      const x = index * step
      const y = height - (point.y / 100) * height
      return `${index === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`
    })
    .join(' ')

  return (
    <div className="chart-shell">
      <svg viewBox={`0 0 ${width} ${height}`} className="h-[180px] w-full" role="img" aria-label="Latency trend">
        <defs>
          <linearGradient id="lineFill" x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor="#16a34a" stopOpacity="0.35" />
            <stop offset="100%" stopColor="#16a34a" stopOpacity="0.03" />
          </linearGradient>
        </defs>
        <path d={`M 0 ${height} ${path} L ${width} ${height} Z`} fill="url(#lineFill)" />
        <path d={path} fill="none" stroke="#0f766e" strokeWidth="3" strokeLinecap="round" className="chart-path" />
      </svg>
    </div>
  )
}
