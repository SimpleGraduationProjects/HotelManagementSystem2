package com.hotel.system.enums;

/**
 * 常用数字
 * @author : wpx
 * @date : 2022/04/30
 */
public enum CommonParamsEnum {

    /**
     * 数字10
     */
    TEN(10),

    /**
     * 数字5
     */
    FIVE(5),

    /**
     * 数字404
     */
    NOT_FOUND(404),

    /**
     * 数字1024
     */
    BYTE(1024);

    private Integer value;

    CommonParamsEnum(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
