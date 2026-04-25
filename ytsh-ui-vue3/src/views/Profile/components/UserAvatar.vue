<template>
  <div class="change-avatar">
    <CropperAvatar
      ref="cropperRef"
      :btnProps="{ preIcon: 'ant-design:cloud-upload-outlined' }"
      :showBtn="false"
      :value="img"
      width="120px"
      @change="handelUpload"
    />
  </div>
</template>
<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'
import { uploadAvatar } from '@/api/system/user/profile'
import { CropperAvatar } from '@/components/Cropper'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'UserAvatar' })

defineProps({
  img: propTypes.string.def('')
})

const userStore = useUserStore()

const cropperRef = ref()
const handelUpload = async ({ data }) => {
  // 文件大小限制 (100KB = 100 * 1024 = 102400 字节)
  const maxSize = 100 * 1024
  if (data.size > maxSize) {
    ElMessage.error('头像文件大小不能超过 100KB，请压缩后重新上传')
    cropperRef.value.close()
    return
  }

  try {
    // 使用专用的头像上传接口 (后端会转为 Base64)
    const response = await uploadAvatar(data)
    const avatar = response.data || response  // 兼容不同的响应格式
    
    // 关闭弹窗，并更新 userStore
    cropperRef.value.close()
    await userStore.setUserAvatarAction(avatar)
    ElMessage.success('头像上传成功')
  } catch (error) {
    ElMessage.error('头像上传失败，请重试')
  }
}
</script>

<style lang="scss" scoped>
.change-avatar {
  img {
    display: block;
    margin-bottom: 15px;
    border-radius: 50%;
  }
}
</style>
