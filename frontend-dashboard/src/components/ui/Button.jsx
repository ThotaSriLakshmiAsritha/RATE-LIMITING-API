export function Button({
  children,
  type = 'button',
  variant = 'primary',
  disabled = false,
  onClick,
  className = '',
}) {
  const variants = {
    primary: 'btn-primary',
    ghost: 'btn-ghost',
    danger: 'btn-danger',
  }

  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className={`btn-base ${variants[variant] || variants.primary} ${className}`.trim()}
    >
      {children}
    </button>
  )
}
