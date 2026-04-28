const dateOnlyPattern = /^(\d{4})-(\d{2})-(\d{2})$/

const formatLocalDate = (date: Date): string | undefined => {
  if (Number.isNaN(date.getTime())) {
    return undefined
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

const normalizeDateOnlyParts = (
  year: number,
  month: number,
  day: number
): string | undefined => {
  const date = new Date(year, month - 1, day)

  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
    return undefined
  }

  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

const normalizeDateOnlyString = (value: string): string | undefined => {
  const match = dateOnlyPattern.exec(value)

  if (!match) {
    return undefined
  }

  return normalizeDateOnlyParts(Number(match[1]), Number(match[2]), Number(match[3]))
}

const normalizeLocalDateArray = (value: unknown[]): string | undefined => {
  if (value.length < 3) {
    return undefined
  }

  const [year, month, day] = value

  if (typeof year !== 'number' || typeof month !== 'number' || typeof day !== 'number') {
    return undefined
  }

  return normalizeDateOnlyParts(year, month, day)
}

export const normalizeDate = (value: unknown): string | undefined => {
  if (value === null || value === undefined) {
    return undefined
  }

  if (typeof value === 'string') {
    const trimmedValue = value.trim()

    if (!trimmedValue) {
      return undefined
    }

    return normalizeDateOnlyString(trimmedValue) ?? formatLocalDate(new Date(trimmedValue))
  }

  if (Array.isArray(value)) {
    return normalizeLocalDateArray(value)
  }

  if (value instanceof Date) {
    return formatLocalDate(value)
  }

  if (typeof value === 'number') {
    return formatLocalDate(new Date(value))
  }

  return undefined
}

export const hasAnyBusinessValue = <T extends object>(
  data: T,
  fields: readonly (keyof T)[]
): boolean => {
  return fields.some((field) => {
    const value = data[field]

    if (value === null || value === undefined) {
      return false
    }

    if (typeof value === 'string') {
      return value.trim().length > 0
    }

    return true
  })
}

export const emptyToNull = <T extends object>(
  data: T,
  fields: readonly (keyof T)[]
): T | null => {
  if (!hasAnyBusinessValue(data, fields)) {
    return null
  }

  return data
}
