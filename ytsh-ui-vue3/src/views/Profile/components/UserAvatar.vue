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
import { compressImage } from '@/utils/compressImage'

defineOptions({ name: 'UserAvatar' })

defineProps({
  img: propTypes.string.def('')
})

const userStore = useUserStore()

const cropperRef = ref()
const handelUpload = async ({ data }) => {
  try {
    const compressedFile = await compressImage(data, { maxWidth: 512, maxHeight: 512 })

    const response = await uploadAvatar(compressedFile)
    const avatar = response.data || response

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
