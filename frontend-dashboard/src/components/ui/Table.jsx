export function Table({ columns, rows, emptyMessage = 'No data yet.' }) {
  return (
    <div className="table-wrap">
      <table className="w-full border-collapse text-left text-sm">
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column.key} className="border-b border-ink-200/60 px-3 py-2 font-semibold text-ink-700">
                {column.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td className="px-3 py-6 text-center text-ink-500" colSpan={columns.length}>
                {emptyMessage}
              </td>
            </tr>
          ) : (
            rows.map((row, index) => (
              <tr key={row.id || `${row.endpoint}-${index}`} className="hover:bg-ink-100/40">
                {columns.map((column) => (
                  <td key={`${column.key}-${index}`} className="border-b border-ink-200/40 px-3 py-2 text-ink-700">
                    {column.render ? column.render(row[column.key], row) : row[column.key]}
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
