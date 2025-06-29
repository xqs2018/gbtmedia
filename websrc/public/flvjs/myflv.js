// FLV播放器实现
class MyFlvPlayer {
    constructor(options = {}) {
        this.options = {
            type: options.type || 'flv',
            url: options.url || '',
            isLive: options.isLive || false,
            hasAudio: options.hasAudio !== false,
            hasVideo: options.hasVideo !== false,
            ...options
        };       
    } 
}

// 导出播放器类
window.myflvjs = {
    createPlayer: MyFlvPlayer.createPlayer,
    isSupported: function() {
        return window.MediaSource !== undefined;
    }
};
