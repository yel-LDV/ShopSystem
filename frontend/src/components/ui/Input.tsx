import { InputHTMLAttributes, forwardRef } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, className = '', ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label className="block text-sm font-medium text-ink-secondary mb-1.5">
            {label}
          </label>
        )}
        <input
          ref={ref}
          className={`
            w-full rounded-sm border bg-surface-tertiary px-3 py-2 text-sm text-ink-primary
            placeholder:text-ink-muted transition-colors duration-150
            focus:outline-none focus:ring-2 focus:ring-accent/30 focus:border-accent
            disabled:opacity-50 disabled:cursor-not-allowed
            ${error ? 'border-semantic-error focus:ring-red-500/30' : 'border-border-standard'}
            ${className}
          `}
          {...props}
        />
        {error && (
          <p className="mt-1 text-xs text-semantic-error">{error}</p>
        )}
      </div>
    )
  }
)

Input.displayName = 'Input'
export default Input
