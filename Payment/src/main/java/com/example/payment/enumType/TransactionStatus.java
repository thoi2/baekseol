package com.example.payment.enumType;

public enum TransactionStatus {
    // 결제 상태
    PENDING("대기중"),
    PROCESSING("처리중"),
    CONFIRMED("확인됨"),
    COMPLETED("완료"),
    
    // 실패
    FAILED("실패"),
    CANCELLED("취소");
    
    private final String description;
    
    TransactionStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
