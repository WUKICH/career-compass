<template>
  <div class="form-container">
    <el-card class="form-card">
      <template #header>
        <div class="card-header">
          <span>用户信息表单</span>
        </div>
      </template>
      
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        label-position="left"
      >
        <el-form-item label="姓名" prop="name">
          <el-input
            v-model="formData.name"
            placeholder="请输入您的姓名"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="年龄" prop="age">
          <el-input-number
            v-model="formData.age"
            :min="1"
            :max="100"
            placeholder="请输入年龄"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="目标岗位" prop="targetJob">
          <el-input
            v-model="formData.targetJob"
            placeholder="例如：Java开发工程师"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="核心技能" prop="skill">
          <el-input
            v-model="formData.skill"
            type="textarea"
            :rows="3"
            placeholder="请输入您的核心技能，多个技能用逗号分隔，例如：Java, Spring Boot, MySQL"
            clearable
          />
        </el-form-item>
        
        <el-form-item label="简历" prop="resume">
          <el-input
            v-model="formData.resume"
            type="textarea"
            :rows="4"
            placeholder="可选：请输入您的简历内容或简历文件路径"
            clearable
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            @click="handleSubmit"
            :loading="isSubmitting"
            size="large"
            style="width: 100%"
          >
            {{ isSubmitting ? '提交中...' : '确认并开始对话' }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { initChatApi } from '@/api/chat';
import { useUserStore } from '@/stores/user';
import { persistWorkflowFromResponse } from '@/utils/workflowPhase';

const router = useRouter();
const formRef = ref(null);
const isSubmitting = ref(false);

const formData = reactive({
  name: '',
  age: null,
  targetJob: '',
  skill: '',
  resume: ''
});

const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  age: [
    { required: true, message: '请输入年龄', trigger: 'blur' },
    { type: 'number', min: 1, max: 100, message: '年龄必须在1-100之间', trigger: 'blur' }
  ],
  targetJob: [
    { required: true, message: '请输入目标岗位', trigger: 'blur' }
  ],
  skill: [
    { required: true, message: '请输入核心技能', trigger: 'blur' }
  ]
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  
  try {
    await formRef.value.validate();
    
    isSubmitting.value = true;
    
    // 调用初始化接口（后端会生成 sessionId）
    const response = await initChatApi({
      name: formData.name,
      age: formData.age,
      targetJob: formData.targetJob,
      skill: formData.skill,
      resume: formData.resume || undefined
    });
    
    if (response && response.code === 1) {
      // 从响应中获取后端生成的 sessionId 和 token
      const sessionId = response.data?.sessionId;
      const token = response.data?.token;
      if (!sessionId) {
        ElMessage.error('服务器未返回 sessionId');
        isSubmitting.value = false;
        return;
      }
      
      // 保存用户信息、sessionId 和 JWT token 到 store
      const userStore = useUserStore();
      // 清除之前的测试完成状态（新会话）
      userStore.clearCompletedTests();
      userStore.setUserInfo({
        sessionId,
        token,  // 保存JWT token
        name: formData.name,
        age: formData.age,
        targetJob: formData.targetJob,
        skill: formData.skill,
        resume: formData.resume
      });
      
      // 同时保存到loginUser，供request.js使用
      if (token) {
        localStorage.setItem('loginUser', JSON.stringify({
          token: token,
          userId: sessionId,
          username: formData.name
        }));
      }
      
      // 保存首条消息到 store（如果有）
      if (response.data && response.data.content) {
        userStore.setInitialMessage(response.data.content);
      }
      persistWorkflowFromResponse(sessionId, response.data);
      
      ElMessage.success('表单提交成功，正在跳转到聊天页面...');
      
      // 跳转到聊天页面
      setTimeout(() => {
        router.push('/chat');
      }, 500);
    } else {
      ElMessage.error(response?.msg || '提交失败，请重试');
    }
  } catch (error) {
    console.error('表单验证或提交失败:', error);
    if (error.message && !error.message.includes('validate')) {
      ElMessage.error(error.message || '提交失败，请重试');
    }
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<style scoped>
.form-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.form-card {
  width: 100%;
  max-width: 600px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

:deep(.el-form-item__label) {
  font-weight: 500;
}
</style>


