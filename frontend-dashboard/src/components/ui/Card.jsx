export function Card({ title, subtitle, actions, children, className = '' }) {
  return (
    <section className={`glass-card ${className}`.trim()}>
      {(title || subtitle || actions) && (
        <header className="mb-4 flex items-start justify-between gap-4">
          <div>
            {title ? <h3 className="text-lg font-semibold text-ink-900">{title}</h3> : null}
            {subtitle ? <p className="mt-1 text-sm text-ink-600">{subtitle}</p> : null}
          </div>
          {actions ? <div>{actions}</div> : null}
        </header>
      )}
      {children}
    </section>
  )
}
