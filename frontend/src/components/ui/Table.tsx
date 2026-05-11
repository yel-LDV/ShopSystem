interface TableProps<T> {
  columns: { key: string; label: string; render?: (item: T) => React.ReactNode }[]
  data: T[]
  onRowClick?: (item: T) => void
  emptyMessage?: string
}

export default function Table<T extends { id: number | string }>({
  columns,
  data,
  onRowClick,
  emptyMessage = 'No hay datos',
}: TableProps<T>) {
  return (
    <div className="overflow-x-auto rounded-md border border-border-standard">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border-standard bg-surface-tertiary/50">
            {columns.map((col) => (
              <th
                key={col.key}
                className="px-4 py-3 text-left font-medium text-ink-tertiary whitespace-nowrap"
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="px-4 py-8 text-center text-ink-muted">
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((item) => (
              <tr
                key={item.id}
                onClick={() => onRowClick?.(item)}
                className={`
                  border-b border-border-soft last:border-0
                  ${onRowClick ? 'cursor-pointer hover:bg-surface-tertiary/50 transition-colors' : ''}
                `}
              >
                {columns.map((col) => (
                  <td key={col.key} className="px-4 py-3 text-ink-primary whitespace-nowrap">
                    {col.render ? col.render(item) : String((item as any)[col.key] ?? '')}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
