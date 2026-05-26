/**
 * 图片压缩工具 - 使用 Canvas API 压缩图片到指定大小以内
 */

export interface CompressOptions {
  maxSize?: number // 目标最大文件大小（字节），默认 1MB
  maxWidth?: number // 最大宽度，默认 1920
  maxHeight?: number // 最大高度，默认 1920
  quality?: number // 初始压缩质量 (0-1)，默认 0.9
  minQuality?: number // 最低压缩质量，默认 0.1
  mimeType?: string // 输出格式，默认 image/jpeg
}

/**
 * 压缩图片文件
 * @param file 原始图片文件（File 或 Blob）
 * @param options 压缩选项
 * @returns 压缩后的 File，如果原文件已小于目标大小则返回原文件
 */
export async function compressImage(
  file: File | Blob,
  options: CompressOptions = {}
): Promise<File> {
  const {
    maxSize = 1024 * 1024, // 1MB
    maxWidth = 1920,
    maxHeight = 1920,
    quality: startQuality = 0.9,
    minQuality = 0.1,
    mimeType = 'image/jpeg'
  } = options

  if (file.size <= maxSize) {
    return file as File
  }

  const image = await loadImage(file)
  const { width, height } = calculateDimensions(
    image.naturalWidth,
    image.naturalHeight,
    maxWidth,
    maxHeight
  )

  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d')!
  ctx.drawImage(image, 0, 0, width, height)

  let quality = startQuality
  let blob = await canvasToBlob(canvas, mimeType, quality)

  while (blob.size > maxSize && quality > minQuality) {
    quality -= 0.1
    blob = await canvasToBlob(canvas, mimeType, quality)
  }

  if (blob.size > maxSize) {
    let scale = 0.9
    while (blob.size > maxSize && scale > 0.2) {
      const newWidth = Math.round(width * scale)
      const newHeight = Math.round(height * scale)
      canvas.width = newWidth
      canvas.height = newHeight
      ctx.drawImage(image, 0, 0, newWidth, newHeight)
      blob = await canvasToBlob(canvas, mimeType, minQuality)
      scale -= 0.1
    }
  }

  const originalName = (file as File).name || 'compressed.jpg'
  const ext = mimeType.split('/')[1] === 'jpeg' ? 'jpg' : mimeType.split('/')[1]
  const baseName = originalName.replace(/\.[^.]+$/, '')
  return new File([blob], `${baseName}.${ext}`, { type: mimeType })
}

function loadImage(file: File | Blob): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const img = new Image()
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片加载失败'))
    }
    img.src = url
  })
}

function calculateDimensions(
  originalWidth: number,
  originalHeight: number,
  maxWidth: number,
  maxHeight: number
): { width: number; height: number } {
  let width = originalWidth
  let height = originalHeight

  if (width > maxWidth) {
    height = Math.round((height * maxWidth) / width)
    width = maxWidth
  }
  if (height > maxHeight) {
    width = Math.round((width * maxHeight) / height)
    height = maxHeight
  }

  return { width, height }
}

function canvasToBlob(canvas: HTMLCanvasElement, mimeType: string, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('Canvas 转 Blob 失败'))
        }
      },
      mimeType,
      quality
    )
  })
}
