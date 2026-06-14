import CryptoJS from 'crypto-js'

/**
 * AES加密工具类
 * 用于加密和解密传输数据，与后端AES加密保持一致
 */

// AES密钥（32字节），需要与后端yml配置保持一致
const AES_KEY = 'career-compass-aes-key-32bytes!!!'

/**
 * 确保密钥长度为32字节（256位），与后端保持一致
 */
function getKey() {
  const keyBytes = CryptoJS.enc.Utf8.parse(AES_KEY)
  // 确保密钥长度为32字节（256位）
  // 如果密钥长度不足32字节，用0填充；如果超过，截取前32字节
  const keyWords = keyBytes.words
  const keyLength = keyBytes.sigBytes
  
  if (keyLength < 32) {
    // 填充到32字节
    const paddedWords = new Array(8) // 32字节 = 8个Word（每个Word 4字节）
    for (let i = 0; i < 8; i++) {
      paddedWords[i] = keyWords[i] || 0
    }
    return CryptoJS.lib.WordArray.create(paddedWords, 32)
  } else if (keyLength > 32) {
    // 截取前32字节
    return CryptoJS.lib.WordArray.create(keyWords.slice(0, 8), 32)
  }
  return keyBytes
}

/**
 * AES加密
 * @param {string} plainText - 明文
 * @returns {string} 加密后的Base64字符串
 */
export function encrypt(plainText) {
  if (!plainText) {
    return plainText
  }
  
  try {
    const key = getKey()
    
    // 使用AES/ECB/PKCS5Padding模式加密
    const encrypted = CryptoJS.AES.encrypt(
      plainText,
      key,
      {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
      }
    )
    
    // 获取纯Base64字符串（与后端格式一致）
    // CryptoJS在ECB模式下，ciphertext就是加密后的数据，需要转换为Base64
    return encrypted.ciphertext.toString(CryptoJS.enc.Base64)
  } catch (error) {
    console.error('AES加密失败:', error)
    throw new Error('数据加密失败')
  }
}

/**
 * AES解密
 * @param {string} encryptedText - 加密后的Base64字符串
 * @returns {string} 解密后的明文
 */
export function decrypt(encryptedText) {
  if (!encryptedText) {
    return encryptedText
  }
  
  try {
    // 清理Base64字符串（移除可能的空白字符）
    const cleanedText = encryptedText.trim().replace(/\s/g, '')
    
    // 验证Base64格式
    if (!cleanedText.match(/^[A-Za-z0-9+/=]+$/)) {
      throw new Error('无效的Base64格式')
    }
    
    const key = getKey()
    
    // 将Base64字符串转换为CipherParams对象
    let ciphertext
    try {
      ciphertext = CryptoJS.enc.Base64.parse(cleanedText)
    } catch (parseError) {
      console.error('Base64解析失败:', parseError)
      throw new Error('Base64解析失败: ' + parseError.message)
    }
    
    const cipherParams = CryptoJS.lib.CipherParams.create({
      ciphertext: ciphertext
    })
    
    const decrypted = CryptoJS.AES.decrypt(
      cipherParams,
      key,
      {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
      }
    )
    
    const plainText = decrypted.toString(CryptoJS.enc.Utf8)
    
    if (!plainText || plainText.length === 0) {
      throw new Error('解密后的数据为空，可能是密钥错误或数据损坏')
    }
    
    return plainText
  } catch (error) {
    console.error('AES解密失败:', {
      error: error.message,
      errorType: error.constructor.name,
      encryptedLength: encryptedText?.length,
      encryptedPreview: encryptedText?.substring(0, 50)
    })
    throw new Error('数据解密失败: ' + error.message)
  }
}
