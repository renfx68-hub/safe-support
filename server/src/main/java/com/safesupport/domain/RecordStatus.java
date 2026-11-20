package com.safesupport.domain;

public enum RecordStatus {
    /** 已预约，等待生效日发送 */
    PENDING,
    /** 已处理完成（全部发送成功或无需发送） */
    SENT,
    /** 重试次数用尽仍有失败 */
    FAILED
}
