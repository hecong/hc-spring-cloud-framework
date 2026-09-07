package com.your.business.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 业务包（com.your.business.dto.）测试 DTO：模拟业务侧实体包，用于白名单追加验证
 */
@Setter
@Getter
public class BizOrderDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String orderNo;
    private List<String> skus;
    private LocalDateTime createdAt;

    public BizOrderDto(String orderNo, List<String> skus, LocalDateTime createdAt) {
        this.orderNo = orderNo;
        this.skus = skus;
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BizOrderDto other)) {
            return false;
        }
        return Objects.equals(orderNo, other.orderNo)
            && Objects.equals(skus, other.skus)
            && Objects.equals(createdAt, other.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderNo, skus, createdAt);
    }
}
