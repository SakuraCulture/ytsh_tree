export const isValidExpressionJsonObject = (value?: string) => {
  if (!value) {
    return false
  }
  try {
    const parsed = JSON.parse(value)
    return !!parsed && !Array.isArray(parsed) && typeof parsed === 'object'
  } catch {
    return false
  }
}
