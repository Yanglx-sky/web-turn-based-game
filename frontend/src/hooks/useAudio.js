import { ref, computed, onMounted, onUnmounted } from 'vue'

/**
 * 音效管理Hook
 * 处理战斗BGM和技能音效播放
 */
export function useAudio() {
  // 音频实例引用
  const bgmRef = ref(null)
  const skillAudioRefs = ref({})
  const isMuted = ref(false)
  const bgmVolume = ref(0.5)
  const effectVolume = ref(0.8)

  // 当前播放状态
  const isPlayingBGM = ref(false)
  const currentBGM = ref('')
  const isAudioEnabled = ref(true)

  // 音效路径配置
  const audioPaths = {
    bgm: {
      battle: '/src/assets/audio/battle/青鸟.mp3',
      battleAlt: '/src/assets/audio/battle/重燃季.mp3'
    },
    battle: {
      start: '/src/assets/audio/battle/战斗开始.mp3',
      end: '/src/assets/audio/battle/结束战斗.mp3'
    },
    skills: {
      fireball: '/src/assets/audio/sasuke/豪火球.mp3',
      waterDragon: '/src/assets/audio/sasuke/水龙弹.mp3'
    }
  }

  // 初始化音频
  const initAudio = () => {
    // 预加载BGM
    bgmRef.value = new Audio()
    bgmRef.value.loop = true
    bgmRef.value.volume = bgmVolume.value

    // 预加载技能音效
    Object.entries(audioPaths.skills).forEach(([key, path]) => {
      skillAudioRefs.value[key] = new Audio(path)
      skillAudioRefs.value[key].volume = effectVolume.value
    })
  }

  // 播放BGM
  const playBGM = (type = 'battle') => {
    if (!isAudioEnabled.value || isMuted.value) return

    const path = audioPaths.bgm[type] || audioPaths.bgm.battle

    if (bgmRef.value) {
      bgmRef.value.src = path
      bgmRef.value.volume = isMuted.value ? 0 : bgmVolume.value
      bgmRef.value.play().catch(err => {
        console.warn('BGM播放失败:', err)
      })
      isPlayingBGM.value = true
      currentBGM.value = type
    }
  }

  // 停止BGM
  const stopBGM = () => {
    if (bgmRef.value) {
      bgmRef.value.pause()
      bgmRef.value.currentTime = 0
      isPlayingBGM.value = false
      currentBGM.value = ''
    }
  }

  // 暂停BGM
  const pauseBGM = () => {
    if (bgmRef.value && isPlayingBGM.value) {
      bgmRef.value.pause()
      isPlayingBGM.value = false
    }
  }

  // 继续BGM
  const resumeBGM = () => {
    if (bgmRef.value && !isMuted.value) {
      bgmRef.value.play().catch(err => {
        console.warn('BGM继续播放失败:', err)
      })
      isPlayingBGM.value = true
    }
  }

  // 播放战斗音效
  const playBattleSound = (type) => {
    if (!isAudioEnabled.value || isMuted.value) return

    const path = audioPaths.battle[type]
    if (path) {
      const audio = new Audio(path)
      audio.volume = effectVolume.value
      audio.play().catch(err => {
        console.warn('音效播放失败:', err)
      })
    }
  }

  // 播放技能音效
  const playSkillSound = (elementType, skillId) => {
    if (!isAudioEnabled.value || isMuted.value) return

    // 根据系别选择音效
    const skillKey = elementType === 1 ? 'fireball' : elementType === 2 ? 'waterDragon' : null

    if (skillKey && skillAudioRefs.value[skillKey]) {
      const audio = skillAudioRefs.value[skillKey]
      audio.currentTime = 0
      audio.volume = effectVolume.value
      audio.play().catch(err => {
        console.warn('技能音效播放失败:', err)
      })
    }
  }

  // 播放攻击音效（普攻）
  const playAttackSound = () => {
    // 播放简单攻击音效
    if (!isAudioEnabled.value || isMuted.value) return

    // 可以添加普攻音效路径
    // const audio = new Audio('/src/assets/audio/attack.mp3')
    // audio.volume = effectVolume.value
    // audio.play()
  }

  // 播放受击音效
  const playHitSound = () => {
    if (!isAudioEnabled.value || isMuted.value) return
    // 可以添加受击音效
  }

  // 设置静音
  const setMuted = (muted) => {
    isMuted.value = muted
    if (bgmRef.value) {
      bgmRef.value.volume = muted ? 0 : bgmVolume.value
    }
  }

  // 设置BGM音量
  const setBGMVolume = (volume) => {
    bgmVolume.value = Math.max(0, Math.min(1, volume))
    if (bgmRef.value && !isMuted.value) {
      bgmRef.value.volume = bgmVolume.value
    }
  }

  // 设置效果音量
  const setEffectVolume = (volume) => {
    effectVolume.value = Math.max(0, Math.min(1, volume))
    Object.values(skillAudioRefs.value).forEach(audio => {
      audio.volume = effectVolume.value
    })
  }

  // 开启/关闭音频
  const toggleAudio = () => {
    isAudioEnabled.value = !isAudioEnabled.value
    if (!isAudioEnabled.value) {
      stopBGM()
    }
  }

  // 初始化
  onMounted(() => {
    initAudio()
  })

  // 清理
  onUnmounted(() => {
    stopBGM()
    if (bgmRef.value) {
      bgmRef.value = null
    }
    Object.values(skillAudioRefs.value).forEach(audio => {
      audio.pause()
    })
    skillAudioRefs.value = {}
  })

  return {
    // 状态
    isMuted,
    bgmVolume,
    effectVolume,
    isPlayingBGM,
    currentBGM,
    isAudioEnabled,

    // 方法
    initAudio,
    playBGM,
    stopBGM,
    pauseBGM,
    resumeBGM,
    playBattleSound,
    playSkillSound,
    playAttackSound,
    playHitSound,
    setMuted,
    setBGMVolume,
    setEffectVolume,
    toggleAudio
  }
}