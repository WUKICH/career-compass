import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref(null);
  // 初始消息
  const initialMessage = ref(null);
  // 聊天记录
  const chatMessages = ref([]);
  // 已完成的测试（skill 或 career）
  const completedTests = ref(new Set());
  // 待显示的职业测试数据（从技能测试提交后返回）
  const pendingCareerTest = ref(null);
  /** 进入 /test 页面临时存放的结构化题目（与 query 解耦） */
  const pendingTestPayload = ref({ skill: null, career: null });
  // 技能测试结果报告
  const skillReport = ref(null);
  // 霍兰德兴趣测试报告（完整 dimensionAnalysis）
  const hollandReport = ref(null);
  // 信息确认页临时数据（从表单跳转 confirm 时使用）
  const pendingConfirmForm = ref(null);
  // 职业综合评估报告
  const careerAssessmentReport = ref(null);
  /** 递增时通知聊天页执行界面级重置（与 performFullLocalReset 配合） */
  const chatRestartTick = ref(0);
  
  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.value = info;
    // 同时保存到 localStorage
    if (info) {
      localStorage.setItem('userInfo', JSON.stringify(info));
      // 如果有 sessionId，用 sessionId 作为 key 保存聊天记录和已完成的测试
      if (info.sessionId) {
        const storageKey = `chatMessages_${info.sessionId}`;
        const storedMessages = localStorage.getItem(storageKey);
        if (storedMessages) {
          try {
            chatMessages.value = JSON.parse(storedMessages);
          } catch (e) {
            console.error('解析聊天记录失败:', e);
            chatMessages.value = [];
          }
        }
        
        // 恢复已完成的测试列表
        const completedTestsKey = `completedTests_${info.sessionId}`;
        const storedCompletedTests = localStorage.getItem(completedTestsKey);
        if (storedCompletedTests) {
          try {
            const testsArray = JSON.parse(storedCompletedTests);
            completedTests.value = new Set(testsArray);
          } catch (e) {
            console.error('解析已完成测试列表失败:', e);
            completedTests.value = new Set();
          }
        }
      }
    } else {
      localStorage.removeItem('userInfo');
    }
  };
  
  // 获取用户信息
  const getUserInfo = () => {
    if (!userInfo.value) {
      // 从 localStorage 恢复
      const stored = localStorage.getItem('userInfo');
      if (stored) {
        try {
          userInfo.value = JSON.parse(stored);
          // 恢复聊天记录和已完成的测试列表
          if (userInfo.value?.sessionId) {
            const storageKey = `chatMessages_${userInfo.value.sessionId}`;
            const storedMessages = localStorage.getItem(storageKey);
            if (storedMessages) {
              try {
                chatMessages.value = JSON.parse(storedMessages);
              } catch (e) {
                console.error('解析聊天记录失败:', e);
                chatMessages.value = [];
              }
            }
            
            // 恢复已完成的测试列表
            const completedTestsKey = `completedTests_${userInfo.value.sessionId}`;
            const storedCompletedTests = localStorage.getItem(completedTestsKey);
            if (storedCompletedTests) {
              try {
                const testsArray = JSON.parse(storedCompletedTests);
                completedTests.value = new Set(testsArray);
              } catch (e) {
                console.error('解析已完成测试列表失败:', e);
                completedTests.value = new Set();
              }
            }
          }
        } catch (e) {
          console.error('解析用户信息失败:', e);
        }
      }
    }
    return userInfo.value;
  };
  
  // 获取 sessionId
  const getSessionId = () => {
    const info = getUserInfo();
    return info?.sessionId || null;
  };
  
  // 设置初始消息
  const setInitialMessage = (message) => {
    initialMessage.value = message;
  };
  
  // 获取初始消息
  const getInitialMessage = () => {
    return initialMessage.value;
  };
  
  // 清除初始消息
  const clearInitialMessage = () => {
    initialMessage.value = null;
  };
  
  // 保存聊天记录
  const saveChatMessages = (messages) => {
    chatMessages.value = messages;
    const sessionId = getSessionId();
    if (sessionId) {
      const storageKey = `chatMessages_${sessionId}`;
      localStorage.setItem(storageKey, JSON.stringify(messages));
    }
  };
  
  // 获取聊天记录
  const getChatMessages = () => {
    return chatMessages.value || [];
  };
  
  // 清除聊天记录
  const clearChatMessages = () => {
    chatMessages.value = [];
    const sessionId = getSessionId();
    if (sessionId) {
      const storageKey = `chatMessages_${sessionId}`;
      localStorage.removeItem(storageKey);
    }
  };
  
  // 标记测试为已完成
  const markTestAsCompleted = (testType) => {
    completedTests.value.add(testType);
    const sessionId = getSessionId();
    if (sessionId) {
      const completedTestsKey = `completedTests_${sessionId}`;
      // 将 Set 转换为数组以便存储
      const testsArray = Array.from(completedTests.value);
      localStorage.setItem(completedTestsKey, JSON.stringify(testsArray));
    }
  };
  
  // 检查测试是否已完成
  const isTestCompleted = (testType) => {
    return completedTests.value.has(testType);
  };
  
  // 清除已完成的测试列表
  const clearCompletedTests = () => {
    completedTests.value = new Set();
    const sessionId = getSessionId();
    if (sessionId) {
      const completedTestsKey = `completedTests_${sessionId}`;
      localStorage.removeItem(completedTestsKey);
    }
  };
  
  // 设置待显示的职业测试数据
  const setPendingCareerTest = (testData) => {
    pendingCareerTest.value = testData;
    const sessionId = getSessionId();
    if (sessionId && testData) {
      try {
        localStorage.setItem(`pendingCareerTest_${sessionId}`, JSON.stringify(testData));
      } catch (e) {
        console.warn('persist pendingCareerTest failed', e);
      }
    }
  };
  
  // 获取待显示的职业测试数据
  const getPendingCareerTest = () => {
    if (pendingCareerTest.value) {
      return pendingCareerTest.value;
    }
    const sessionId = getSessionId();
    if (!sessionId) {
      return null;
    }
    try {
      const raw = localStorage.getItem(`pendingCareerTest_${sessionId}`);
      if (!raw) {
        return null;
      }
      const parsed = JSON.parse(raw);
      pendingCareerTest.value = parsed;
      return parsed;
    } catch {
      return null;
    }
  };
  
  // 清除待显示的职业测试数据
  const clearPendingCareerTest = () => {
    pendingCareerTest.value = null;
    const sessionId = getSessionId();
    if (sessionId) {
      localStorage.removeItem(`pendingCareerTest_${sessionId}`);
    }
  };

  const setPendingTestPayload = (kind, payload) => {
    if (kind !== 'skill' && kind !== 'career') return;
    pendingTestPayload.value = { ...pendingTestPayload.value, [kind]: payload };
  };

  const consumePendingTestPayload = (kind) => {
    if (kind !== 'skill' && kind !== 'career') return null;
    const p = pendingTestPayload.value[kind];
    pendingTestPayload.value = { ...pendingTestPayload.value, [kind]: null };
    return p;
  };

  const clearPendingTestPayload = () => {
    pendingTestPayload.value = { skill: null, career: null };
  };

  // 设置技能测试结果报告
  const setSkillReport = (report) => {
    skillReport.value = report;
    const sessionId = getSessionId();
    if (sessionId) {
      localStorage.setItem(`skillReport_${sessionId}`, JSON.stringify(report));
    }
  };

  const getSkillReport = () => {
    if (!skillReport.value) {
      const sessionId = getSessionId();
      if (sessionId) {
        const stored = localStorage.getItem(`skillReport_${sessionId}`);
        if (stored) {
          try {
            skillReport.value = JSON.parse(stored);
          } catch (e) {
            console.error('解析技能测试报告失败:', e);
          }
        }
      }
    }
    return skillReport.value;
  };

  const clearSkillReport = () => {
    skillReport.value = null;
    const sessionId = getSessionId();
    if (sessionId) {
      localStorage.removeItem(`skillReport_${sessionId}`);
    }
  };

  const setHollandReport = (report) => {
    hollandReport.value = report;
    const sessionId = getSessionId();
    if (sessionId && report) {
      localStorage.setItem(`hollandReport_${sessionId}`, JSON.stringify(report));
    }
  };

  const getHollandReport = () => {
    if (!hollandReport.value) {
      const sessionId = getSessionId();
      if (sessionId) {
        const stored = localStorage.getItem(`hollandReport_${sessionId}`);
        if (stored) {
          try {
            hollandReport.value = JSON.parse(stored);
          } catch (e) {
            console.error('解析霍兰德报告失败:', e);
          }
        }
      }
    }
    return hollandReport.value;
  };

  const clearHollandReport = () => {
    hollandReport.value = null;
    const sessionId = getSessionId();
    if (sessionId) {
      localStorage.removeItem(`hollandReport_${sessionId}`);
    }
  };

// 设置职业综合评估报告
const setCareerAssessmentReport = (report) => {
  careerAssessmentReport.value = report;
  const sessionId = getSessionId();
  if (sessionId && report) {
    localStorage.setItem(`careerAssessmentReport_${sessionId}`, JSON.stringify(report));
  }
};

// 获取职业综合评估报告
const getCareerAssessmentReport = () => {
  if (!careerAssessmentReport.value) {
    const sessionId = getSessionId();
    if (sessionId) {
      const stored = localStorage.getItem(`careerAssessmentReport_${sessionId}`);
      if (stored) {
        try {
          careerAssessmentReport.value = JSON.parse(stored);
        } catch (e) {
          console.error('解析职业评估报告失败:', e);
        }
      }
    }
  }
  return careerAssessmentReport.value;
};

// 清除职业综合评估报告
const clearCareerAssessmentReport = () => {
  careerAssessmentReport.value = null;
  const sessionId = getSessionId();
  if (sessionId) {
    localStorage.removeItem(`careerAssessmentReport_${sessionId}`);
  }
};

  // 设置信息确认页临时数据（持久化到 localStorage，支持新用户）
  const setPendingConfirmForm = (data) => {
    pendingConfirmForm.value = data;
    // 持久化到 localStorage，使用固定 key（不依赖 sessionId，因为新用户可能还没有 sessionId）
    if (data) {
      localStorage.setItem('pendingConfirmForm', JSON.stringify(data));
    } else {
      localStorage.removeItem('pendingConfirmForm');
    }
  };

  // 获取信息确认页临时数据（优先从内存，否则从 localStorage 恢复）
  const getPendingConfirmForm = () => {
    if (pendingConfirmForm.value) {
      return pendingConfirmForm.value;
    }
    // 从 localStorage 恢复
    const stored = localStorage.getItem('pendingConfirmForm');
    if (stored) {
      try {
        pendingConfirmForm.value = JSON.parse(stored);
        return pendingConfirmForm.value;
      } catch (e) {
        console.error('解析 pendingConfirmForm 失败:', e);
        localStorage.removeItem('pendingConfirmForm');
      }
    }
    return null;
  };

  // 清除信息确认页临时数据
  const clearPendingConfirmForm = () => {
    pendingConfirmForm.value = null;
    localStorage.removeItem('pendingConfirmForm');
  };

  const hasPendingCollectConfirm = () => {
    const pending = getPendingConfirmForm();
    return !!(pending && pending.fromCollect);
  };

  // 检查是否已显示过欢迎消息（使用 localStorage 持久化，全局唯一）
  const hasShownWelcomeMessage = () => {
    return localStorage.getItem('hasShownWelcomeMessage') === 'true';
  };

  // 标记已显示欢迎消息
  const markWelcomeMessageShown = () => {
    localStorage.setItem('hasShownWelcomeMessage', 'true');
  };
  
  /** 清除指定 session 在 localStorage 中的键（需在 clearAll 前调用并传入 sessionId） */
  const purgeSessionScopedStorage = (sessionId) => {
    if (!sessionId) return;
    const keys = [
      `chatMessages_${sessionId}`,
      `completedTests_${sessionId}`,
      `skillReport_${sessionId}`,
      `hollandReport_${sessionId}`,
      `careerAssessmentReport_${sessionId}`
    ];
    keys.forEach((k) => {
      try {
        localStorage.removeItem(k);
      } catch (e) {
        console.warn('removeItem failed', k, e);
      }
    });
  };

  /** 清除本地存储与 Pinia 用户状态，并通知聊天页回到欢迎页 */
  const performFullLocalReset = async () => {
    const sid = getSessionId();
    purgeSessionScopedStorage(sid);
    try {
      localStorage.removeItem('loginUser');
      localStorage.removeItem('userInfo');
      try {
        sessionStorage.clear();
      } catch (e) {
        /* ignore */
      }
      try {
        if (typeof caches !== 'undefined' && caches.keys) {
          const names = await caches.keys();
          await Promise.all(names.map((n) => caches.delete(n)));
        }
      } catch (e) {
        console.debug('Cache API 清理跳过', e);
      }
      clearAll();
      chatRestartTick.value += 1;
    } catch (e) {
      console.warn('清除本地数据失败', e);
    }
  };

  // 清除所有信息
  const clearAll = () => {
    userInfo.value = null;
    initialMessage.value = null;
    clearChatMessages();
    clearCompletedTests();
    clearPendingCareerTest();
    clearPendingTestPayload();
    clearSkillReport();
    clearHollandReport();
    clearPendingConfirmForm();
  clearCareerAssessmentReport();
    localStorage.removeItem('userInfo');
    // 注意：不清除 hasShownWelcomeMessage，因为这是全局标记，不应该在清除用户数据时重置
  };
  
  return {
    userInfo,
    initialMessage,
    chatMessages,
    completedTests,
    pendingCareerTest,
    pendingTestPayload,
    skillReport,
    hollandReport,
    setUserInfo,
    getUserInfo,
    getSessionId,
    setInitialMessage,
    getInitialMessage,
    clearInitialMessage,
    saveChatMessages,
    getChatMessages,
    clearChatMessages,
    markTestAsCompleted,
    isTestCompleted,
    clearCompletedTests,
    setPendingCareerTest,
    getPendingCareerTest,
    clearPendingCareerTest,
    setPendingTestPayload,
    consumePendingTestPayload,
    clearPendingTestPayload,
    setSkillReport,
    getSkillReport,
    clearSkillReport,
    setHollandReport,
    getHollandReport,
    clearHollandReport,
  setCareerAssessmentReport,
  getCareerAssessmentReport,
  clearCareerAssessmentReport,
    setPendingConfirmForm,
    getPendingConfirmForm,
    clearPendingConfirmForm,
    hasPendingCollectConfirm,
    hasShownWelcomeMessage,
    markWelcomeMessageShown,
    clearAll,
    chatRestartTick,
    purgeSessionScopedStorage,
    performFullLocalReset
  };
});


