/**
 * 测试 normalizeDate 函数的边界情况
 * 用于诊断日期回显问题
 */

// 复制 normalizeDate 函数的实现（去掉 TypeScript 类型标注）
const normalizeDate = (date) => {
  if (!date) return undefined
  
  // 如果已经是 YYYY-MM-DD 格式，直接返回
  if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
    return date
  }
  
  // 如果是 ISO 格式（带时间），提取日期部分
  if (typeof date === 'string' && date.includes('T')) {
    return date.split('T')[0]
  }
  
  // 如果是时间戳，转换为日期
  if (typeof date === 'number') {
    const d = new Date(date)
    if (!isNaN(d.getTime())) {
      return d.toISOString().split('T')[0]
    }
  }
  
  // 如果是 Date 对象
  if (date instanceof Date) {
    return date.toISOString().split('T')[0]
  }
  
  // 尝试作为字符串解析
  if (typeof date === 'string') {
    const d = new Date(date)
    if (!isNaN(d.getTime())) {
      return d.toISOString().split('T')[0]
    }
  }
  
  // 无法解析，返回 undefined
  console.warn('[normalizeDate] 无法解析日期:', date)
  return undefined
}

// 测试用例
const testCases = [
  // 正常格式
  { input: "2023-08-15", description: "正常 YYYY-MM-DD 格式" },
  
  // ISO 格式（带时间）
  { input: "2023-08-15T00:00:00", description: "ISO 格式（带时间）" },
  { input: "2023-08-15T08:30:00", description: "ISO 格式（带时间和分钟）" },
  { input: "2023-08-15T08:30:45.123", description: "ISO 格式（带毫秒）" },
  
  // 时间戳
  { input: 1692019200000, description: "毫秒时间戳" },
  { input: 1692019200, description: "秒时间戳" },
  
  // 其他字符串格式
  { input: "2023/08/15", description: "斜杠分隔" },
  { input: "15/08/2023", description: "日/月/年格式" },
  { input: "08-15-2023", description: "月-日-年格式" },
  
  // 空值
  { input: null, description: "null" },
  { input: undefined, description: "undefined" },
  { input: "", description: "空字符串" },
  
  // 对象类型
  { input: new Date("2023-08-15"), description: "Date 对象" },
]

console.log("🧪 normalizeDate 函数测试：")
console.log("=".repeat(80))

testCases.forEach(test => {
  const result = normalizeDate(test.input)
  const status = result !== undefined ? "✅" : "❌"
  console.log(`${status} ${test.description}`)
  console.log(`   输入: ${JSON.stringify(test.input)}`)
  console.log(`   输出: ${JSON.stringify(result)}`)
  console.log("-".repeat(80))
})
